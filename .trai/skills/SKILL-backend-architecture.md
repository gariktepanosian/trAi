# SKILL: Backend Architecture
# TrAI Platform — Spring Boot 4 / Java 21

## What this skill covers

This skill governs **all backend Java code** in `backend/src/main/java/com/trai/engine/`.
Read this before adding any new package, class, controller, service, or entity.

---

## Stack & Versions

| Technology | Version | Notes |
|---|---|---|
| Spring Boot | 4.0.3 | `build.gradle` root plugin |
| Java | 21 | Virtual threads enabled (`Executors.newVirtualThreadPerTaskExecutor()`) |
| MongoDB | 7.0 | Primary data store via `spring-boot-starter-data-mongodb` |
| Redis | 7.2 | Cache layer via `spring-boot-starter-data-redis` + Bucket4j rate limiting |
| LangChain4j | 0.29.1 | AI abstraction layer |
| JJWT | 0.12.6 | JWT auth |
| Jsoup | 1.17.2 | News scraping |
| OkHttp | 4.12.0 | Twitter API v2 HTTP calls |
| Bucket4j | 8.10.1 | Rate limiting |
| Firebase Admin | 9.3.0 | Push notifications (FCM) |
| Google Cloud Storage | 2.40.1 | Media uploads |

---

## Package Layout

Every feature lives in its own package under `com.trai.engine.<feature>/`.
**Never** dump classes into a generic `util/` or `common/` package.

```
com.trai.engine/
├── ai/              # LangChain4j @SystemMessage interfaces (AiServices proxies)
├── alert/           # PlatformAlert domain + service + repository
├── analytics/       # MarketImpactEngine + PredictiveAnalyticsService
├── audit/           # Audit log domain + service + REST controller
├── config/          # @Configuration beans only (AiConfig, RedisConfig, I18nConfig)
├── controller/      # @RestController only — no business logic here
├── country/         # Country monitoring: service + controller + result
├── domain/          # MongoDB @Document models (no business logic)
├── dto/             # Request/response DTOs (plain POJOs, no annotations)
├── guardrail/       # OutputGuardrailsService — output safety evaluation
├── multiagent/      # MultiAgentValidationService + AgentOutput + MultiAgentResult
├── notifications/   # PushNotificationService (Firebase FCM)
├── repository/      # MongoRepository interfaces only
├── sanitizer/       # InputSanitizerService — input validation + PII masking
├── scheduler/       # @Scheduled tasks (scraping, trust recalculation)
├── search/          # In-memory / future Elasticsearch search
├── security/        # JWT filter + SecurityConfig + rate limiting
├── service/         # Core orchestration services (TrustVerificationService)
├── translation/     # TranslationService
├── twitter/         # TwitterApiService + TweetData
└── webhook/         # B2B webhook partner management
```

---

## Naming Conventions

### Classes
- Controllers: `<Feature>Controller.java` — `@RestController`, `@RequestMapping("/api/v1/<feature>")`
- Services: `<Feature>Service.java` — `@Service`, business logic
- Repositories: `<Feature>Repository.java` — `MongoRepository<T, String>` interface
- Domain models: plain noun, `@Document(collection = "<plural_snake_case>")`
- DTOs: `<Action>Request.java` / `<Action>Response.java`
- Schedulers: `<Feature>Scheduler.java` — `@Service` + `@Scheduled`
- Config: `<Feature>Config.java` — `@Configuration`

### Methods
- Controller methods: `verbNoun()` — `verifyLiveStatement()`, `subscribeToCountry()`
- Service methods: descriptive verb phrase — `triggerCriticalAlert()`, `buildAggregatedClaim()`
- Private helpers: prefix with `build`, `extract`, `map`, `safe`, `fallback`

### REST Endpoints
All endpoints follow: `/api/v1/<feature>/<action>`
- GET for reads: `/api/v1/alerts`, `/api/v1/news`, `/api/v1/trust/status`
- POST for actions: `/api/v1/live/verify-statement`, `/api/v1/country/subscribe`
- No DELETE or PATCH endpoints currently — use POST with explicit action names

---

## Controller Pattern

```java
// CORRECT — controller is thin, delegates to service
@RestController
@RequestMapping("/api/v1/feature")
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService; // constructor injection ONLY
    }

    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> doAction(@RequestBody FeatureRequest request) {
        Map<String, Object> result = featureService.doAction(request);
        return ResponseEntity.ok(result);
    }
}

// WRONG — no @Autowired, no logic in controller, no @Value in controller
```

---

## Service Pattern

```java
@Service
public class FeatureService {

    private static final Logger log = LoggerFactory.getLogger(FeatureService.class);

    // All dependencies via constructor injection
    public FeatureService(Dep1 dep1, Dep2 dep2) { ... }

    public Map<String, Object> doAction(FeatureRequest req) {
        // 1. Kill-switch check (if AI-dependent)
        // 2. Input sanitization
        // 3. Core logic with try/catch + fallback
        // 4. Guardrail evaluation (if AI output)
        // 5. Audit log
        // 6. Return result
    }

    // Every public method that can fail needs a private fallback*()
    private Map<String, Object> fallbackDoAction(...) { ... }
}
```

---

## MongoDB Domain Model Pattern

```java
@Document(collection = "feature_items")  // snake_case plural
public class FeatureItem {

    @Id
    private String id;
    private String fieldName;      // camelCase fields
    private Instant createdAt;     // always include createdAt

    public FeatureItem() {
        this.createdAt = Instant.now(); // set in constructor
    }

    // Only getters and setters — no business logic in domain classes
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // ...
}
```

---

## Error Handling Rules

1. **Never let exceptions propagate to the HTTP layer** — all service methods must have `try/catch` with a fallback result
2. **Never return HTTP 500** — always return a structured JSON with `"status": "ERROR"` and a human-readable message
3. **Log warnings** (`log.warn`) for recoverable failures (AI API down, network timeout)
4. **Log errors** (`log.error`) only for data persistence failures or unexpected exceptions
5. **Log info** (`log.info`) for major pipeline steps (start of scraping, multi-agent start/end, push sent)

---

## Caching Rules

- Use `@Cacheable` only on methods that call AI APIs or do expensive DB reads
- Cache names: `"normalized-news"`, `"source-trust-scores"` (defined in `application.yml`)
- Cache TTL: 10 minutes in production (`application-prod.yml`)
- Never cache methods that write to the database

---

## Configuration Rules

- All secrets in environment variables — **never hardcode** API keys
- All `@Value` fields have a safe default: `@Value("${xai.api-key:demo-key}")`
- Add new env vars to `.env.example` with clear comments
- Add new config properties to both `application.yml` (dev defaults) and `application-prod.yml` (env var refs)

---

## Playwright Test Coverage Trigger

When you add or modify a backend feature, the following Playwright tests MUST be updated or created:

| Change type | Required Playwright coverage |
|---|---|
| New REST endpoint | `tests/api/<feature>.spec.ts` — test happy path + error cases |
| New AI engine method | Mock AI response in `tests/api/ai-*.spec.ts` |
| New scheduler | Not directly testable with Playwright — use unit test |
| New domain field | Update existing spec to assert new field in API response |
| Changed endpoint URL | Update all specs referencing old URL |

See `SKILL-playwright-testing.md` for the full Playwright spec format.
