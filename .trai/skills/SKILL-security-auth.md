# SKILL: Security & Authentication
# TrAI Platform — JWT, Spring Security, Rate Limiting, Input Sanitizer

## What this skill covers

JWT authentication flow, Spring Security configuration, rate limiting with Bucket4j,
input sanitization rules, and guidelines for securing new endpoints.

---

## File Locations

```
backend/src/main/java/com/trai/engine/security/
├── SecurityConfig.java          # Spring Security filter chain + CORS
├── JwtUtil.java                 # JWT create / validate / extract
├── JwtAuthFilter.java           # OncePerRequestFilter — validates JWT on every request
├── RateLimitingFilter.java      # Bucket4j rate limiter (per IP)
└── TraiUserDetailsService.java  # Loads UserDetails from MongoDB

backend/src/main/java/com/trai/engine/sanitizer/
└── InputSanitizerService.java   # Input validation, PII masking, injection detection

backend/src/main/java/com/trai/engine/controller/
└── AuthController.java          # POST /api/v1/auth/register + /api/v1/auth/login
```

---

## JWT Flow

```
1. POST /api/v1/auth/login  { username, password }
        │
        └── AuthController → TraiUserDetailsService.loadUserByUsername()
            → BCrypt password verify
            → JwtUtil.generateToken(username)
            → Response: { "token": "eyJ..." }

2. All subsequent requests:
   Header: Authorization: Bearer eyJ...
        │
        └── JwtAuthFilter.doFilterInternal()
            → JwtUtil.extractUsername(token)
            → JwtUtil.validateToken(token, userDetails)
            → SecurityContextHolder.setAuthentication(...)
            → Request proceeds to controller
```

### JWT Configuration
```yaml
jwt:
  secret: ${JWT_SECRET}           # Min 256-bit random string — never commit
  expiration-ms: 86400000         # 24 hours default
```

Token expiry is read from `JwtUtil` via `@Value("${jwt.expiration-ms:86400000}")`.

---

## Spring Security Rules

### Public endpoints (no JWT required)
```java
// In SecurityConfig.java — requestMatchers that are permitAll()
"/api/v1/auth/**"      // Login + register
"/api/v1/trust/status" // Health check (used by Flutter status indicator)
"/api/v1/alerts"       // Read-only alerts (public visibility)
"/api/v1/news"         // Public news feed
```

### Protected endpoints (JWT required)
Everything else: `/api/v1/live/**`, `/api/v1/trust/**`, `/api/v1/validate/**`,
`/api/v1/country/**`, `/api/v1/analytics/**`, `/api/v1/audit/**`

### Adding a new public endpoint
Edit `SecurityConfig.java` — add to the `permitAll()` block:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/new-public-endpoint").permitAll()
```

### Adding a new protected endpoint
Do nothing — the default is to require authentication. Just add the controller method.

---

## Rate Limiting

Uses Bucket4j with per-IP buckets (currently in-memory `ConcurrentHashMap`).

### Current configuration (in `RateLimitingFilter.java`)
- **20 requests per minute** per IP address
- Returns HTTP 429 with `Retry-After` header when exceeded
- Bucket is created on first request from each IP

### Known limitation
Rate limit buckets are **in-memory only** and reset on server restart.
In multi-instance Cloud Run deployments, each instance has its own bucket map.
To fix: use `bucket4j-redis` to store buckets in Redis (dependency already in `build.gradle`).

### Modifying rate limits
Edit `RateLimitingFilter.java`, the `newBucket()` method:
```java
// Example: Change to 50 requests per minute
Bandwidth limit = Bandwidth.classic(50, Refill.greedy(50, Duration.ofMinutes(1)));
```

---

## Input Sanitizer

`InputSanitizerService.sanitize(input)` returns `SanitizationResult`:

```java
record SanitizationResult(
    boolean blocked,      // true = input was rejected
    String cleanText,     // sanitized text safe to pass to AI
    List<String> flags    // reasons why input was flagged/modified
) {}
```

### What it checks
- **Prompt injection patterns:** `"ignore previous instructions"`, `"DAN mode"`, `"jailbreak"`, etc.
- **XSS patterns:** `<script>`, `javascript:`, HTML tags
- **SQL injection:** `' OR 1=1`, `DROP TABLE`, etc.
- **Excessive length:** > 10,000 characters → blocked
- **PII email masking:** emails replaced with `[EMAIL_REDACTED]`
- **Known issue:** Phone number pattern is defined but not applied (bug — add phone masking to `sanitize()` method)

### Sanitizer usage pattern
```java
SanitizationResult sanitized = inputSanitizerService.sanitize(req.getInput());
if (sanitized.blocked()) {
    return blockedInputResponse("FEATURE_NAME", sanitized);
}
// use sanitized.cleanText() for all AI calls
```

**Rule:** Every user-supplied input to an AI engine MUST pass through the sanitizer first.

---

## Output Guardrails

`OutputGuardrailsService.evaluate(aiOutput, riskScore)` returns `GuardrailResult`:

```java
record GuardrailResult(
    boolean blocked,
    String safeOutput,     // replacement text if blocked
    List<String> flags
) {}
```

### When to use guardrails
Apply after every AI response before returning to the client:
```java
GuardrailResult guardrail = outputGuardrailsService.evaluate(aiOutput, riskScore);
result.put("guardrailFlags", guardrail.flags());
result.put("guardrailBlocked", guardrail.blocked());
if (guardrail.blocked()) {
    result.put("analysis", guardrail.safeOutput());
}
```

---

## CORS Configuration

```java
// In SecurityConfig.java
config.setAllowedOriginPatterns(List.of("*"));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
```

**Known security issue:** Wildcard origins with `allowCredentials=true` is unsafe for production.
Before production deployment, restrict `setAllowedOriginPatterns` to specific domains:
```java
config.setAllowedOriginPatterns(List.of(
    "https://trai-frontend-*.run.app",
    "https://yourdomain.com"
));
```

---

## Audit Logging

Every AI operation is recorded via `AuditLogService.record(...)`:

```java
auditLogService.record(
    currentActor(),      // username from JWT or "anonymous"
    "ACTION_TYPE",       // e.g. "LIVE_FACT_CHECK", "NEWS_VERIFY"
    inputText,           // sanitized input text
    verdict,             // AI output verdict
    trustScore,          // integer 0-100 or null
    flags,               // combined sanitizer + guardrail flags
    blocked,             // whether guardrail blocked the response
    elapsedMs            // processing time
);
```

The audit log is stored in MongoDB `audit_logs` collection.
Accessible via `GET /api/v1/audit` (requires JWT, admin role).

---

## Playwright Test Coverage Trigger

After any change to security configuration:

- `tests/api/auth.spec.ts` — test register + login + token usage + invalid token rejection
- `tests/api/rate-limiting.spec.ts` — fire 21 rapid requests, assert 20th succeeds and 21st returns 429
- `tests/api/sanitizer.spec.ts` — POST with prompt injection payload, assert `"status": "BLOCKED"`
