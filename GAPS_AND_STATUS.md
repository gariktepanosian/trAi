# TrAI Platform — Gaps, Incomplete Aspects & Status Audit
> **Date:** September 2026  
> **Auditor:** Full codebase and blueprint review  
> **Project:** trAi — Trust-as-a-Service Infrastructure Platform

---

## LEGEND

| Symbol | Meaning |
|--------|---------|
| ✅ | Done — fully implemented |
| ⚠️ | Partial — exists but incomplete |
| ❌ | Missing — not implemented yet |
| 🔵 | Architecture-only — documented but no code |

---

## 1. BACKEND — Spring Boot Core

| Component | Status | Notes |
|-----------|--------|-------|
| Spring Boot project scaffold | ✅ | `backend/` with Gradle, running on port 8080 |
| MongoDB connection | ✅ | `application.yml` configured, `MongoRepository` used |
| Redis connection | ✅ | Docker Compose includes Redis; `application.yml` has host/port |
| `TrustEngineController` (REST) | ✅ | Exposes `/api/v1/trust/*` and `/api/v1/live/*` |
| `TrustVerificationService` | ✅ | Orchestrates all 3 AI engines |
| `AntiPropagandaEngine` (LangChain4j) | ✅ | Interface wired via `AiConfig` |
| `LiveFactCheckEngine` (LangChain4j) | ✅ | Interface wired via `AiConfig` |
| `AiGlitchVerifierEngine` (LangChain4j) | ✅ | Interface wired via `AiConfig` |
| `NewsScrapingScheduler` | ✅ | Jsoup-based, runs every 10 min |
| MongoDB domain models | ✅ | `NormalizedNews`, `SourceTrustScore`, `InsiderInfo`, `VerifiedClaim` |
| MongoDB repositories | ✅ | `NormalizedNewsRepository`, `SourceTrustScoreRepository` |
| DTOs | ✅ | `LiveStatementRequest`, `AiGlitchCheckRequest`, `NewsVerificationRequest` |
| **Package naming** | ⚠️ | Still uses `com.antigravity.engine` — **must be renamed to `com.trai.engine`** |
| **API Gateway microservice** | ❌ | Not created. A single Spring Boot app exists; no Spring Cloud Gateway module |
| **auth-service microservice** | ❌ | No authentication module. No JWT, no Spring Security, no login endpoint |
| **User registration / login** | ❌ | No user entity, no signup, no session management |
| **Webhook catcher service** | ❌ | Completely missing. No `/webhook` endpoint, no B2B JSON ingestion logic |
| **Liquibase migrations** | ❌ | Not present. No `db/changelog/` directory, no XML/YAML changelogs. MongoDB-only setup (schema-less) but Liquibase was in requirements |
| **Internationalization (i18n)** | ❌ | No `messages.properties`, no `messages_ru.properties`, no `messages_am.properties`. No `LocaleResolver` bean configured |
| **Input sanitization / Security layer** | ❌ | No prompt injection detection, no PII masking, no HTML/SQL stripping on the input side (as specified in TrustAI Security Gateway section) |
| **Kill switch / Human-in-the-loop** | ❌ | SentinelMind kill switch described in blueprint but not implemented |
| **Redis caching layer** | ⚠️ | Redis is in Docker Compose and application.yml but no `@Cacheable` or `RedisTemplate` usage in code |
| **Output guardrails** | ⚠️ | AI glitch auditor exists but no formal output filter blocking responses above risk threshold |
| **Audit log / Report generation** | ❌ | No `AuditReportService`, no PDF export (iText / PDFBox), no audit trail endpoints |
| **Rate limiting** | ❌ | No rate limiting on any endpoint (no Bucket4j, no API Gateway filter) |
| **Elasticsearch integration** | ❌ | Mentioned in blueprint as required for full-text news search; not implemented |
| **DeepL / Translation pipeline** | ❌ | Multi-language translation service not implemented |
| **Trust score recalculation scheduler** | ⚠️ | `NewsScrapingScheduler` scrapes but no dedicated trust score recalculation job |
| **Predictive analytics service** | ❌ | Market impact prediction (gold/BTC/oil) described in blueprint, not implemented |
| **`application-prod.yml`** | ❌ | No production profile file with Yandex Cloud placeholders |
| **Yandex Cloud / Docker deployment** | ❌ | Dockerfile missing from `backend/`. `docker-compose.yml` only covers infra (Mongo + Redis), not the app container |
| **Unit tests** | ❌ | `src/test/` directory exists but no test classes created |
| **CORS configuration** | ❌ | Frontend calls `localhost:8080` but no `@CrossOrigin` or `CorsConfigurationSource` bean |

---

## 2. FRONTEND — Web (HTML/CSS/JS)

| Component | Status | Notes |
|-----------|--------|-------|
| `index.html` landing page | ✅ | 4-tab layout: Live Fact-Check, AI Auditor, News Normalizer, Source Index |
| `style.css` | ✅ | Dark theme, Space Grotesk font, responsive layout |
| `app.js` — Tab switching | ✅ | Works correctly |
| `app.js` — Backend health check | ✅ | Polls `/api/v1/trust/status`, falls back gracefully |
| `app.js` — Live statement verification | ✅ | Calls backend OR falls back to client-side simulation |
| `app.js` — AI glitch auditor | ✅ | Calls backend OR falls back to client-side simulation |
| `app.js` — News propaganda stripper | ⚠️ | UI exists, but **always uses client-side simulation** — never calls backend |
| `app.js` — Source trust table | ⚠️ | Loads from backend if available, falls back to hardcoded 6-source mock |
| **Framework upgrade (React/TypeScript)** | ❌ | Blueprint specifies React 18 + TypeScript + Tailwind CSS. Current implementation is plain HTML/JS (acceptable for demo, not production) |
| **n8n-style pipeline builder UI** | ❌ | Node canvas drag-and-drop pipeline builder not implemented anywhere |
| **User authentication UI** | ❌ | No login/signup screen |
| **Alert/notification UI** | ❌ | Push notification management screen not implemented |
| **Predictive analytics charts** | ❌ | Charts for market impact predictions not present |
| **Mobile (iOS / Android / Flutter)** | ❌ | No Flutter project created anywhere |
| **Webhook dashboard for B2B** | ❌ | No B2B UI showing incoming webhook requests |

---

## 3. MOBILE — iOS / Android

| Component | Status | Notes |
|-----------|--------|-------|
| Flutter project scaffold | ❌ | Does not exist |
| iOS app | ❌ | Does not exist |
| Android app | ❌ | Does not exist |
| Push notification integration (FCM) | ❌ | Does not exist |

---

## 4. INFRASTRUCTURE & DEVOPS

| Component | Status | Notes |
|-----------|--------|-------|
| `docker-compose.yml` for MongoDB + Redis | ✅ | Correct, minimal, usable |
| `Dockerfile` for Spring Boot backend | ❌ | Not present in `backend/` |
| `Dockerfile` for frontend | ❌ | Not present in `frontend/` |
| Yandex API Gateway config (YAML spec) | ❌ | Not created |
| Yandex Cloud Lockbox integration | ❌ | Only placeholder env var syntax in `application.yml` |
| Kubernetes manifests (k8s YAML) | ❌ | Not present |
| CI/CD pipeline (GitHub Actions / Yandex) | ❌ | Not present |
| `.gitignore` | ✅ | Present |

---

## 5. SECURITY

| Component | Status | Notes |
|-----------|--------|-------|
| API keys in environment variables | ⚠️ | `${XAI_API_KEY}` and `${GEMINI_PROJECT_ID}` syntax used but no `.env.example` file |
| Spring Security | ❌ | No dependency, no config |
| JWT authentication | ❌ | Not implemented |
| HTTPS / TLS | ❌ | No configuration |
| `.env` or secrets file in `.gitignore` | ⚠️ | `.gitignore` exists but no documented secrets management |

---

## 6. B2B WEBHOOK CATCHER (Critical Missing Piece)

This was explicitly requested and is **completely missing**.

### What needs to be built:

```
External System / Partner App
        |
        | POST /api/v1/webhook/ingest
        | (JSON payload)
        |
   [TrAI Gateway decides:]
   - What type of request is it? (news feed? AI output audit? live statement?)
   - Route to correct internal service
   - Return structured JSON response
        |
   [Response sent back to caller]
```

### Missing endpoints:
- `POST /api/v1/webhook/ingest` — Main B2B entry point
- `GET /api/v1/webhook/status/{requestId}` — Async result polling
- `POST /api/v1/webhook/register` — B2B partner registration
- Webhook signature verification (HMAC-SHA256)
- Request type classifier (routes to fact-check, hallucination audit, or news normalization)

---

## 7. B2B vs B2C DUAL-SIDE ARCHITECTURE (Missing)

The blueprint specifies the product serves two audiences:

| Side | Users | What They Need |
|------|-------|----------------|
| **B2C (Consumer)** | Journalists, individuals | News feed, alerts, topic pipelines, mobile app |
| **B2B (Enterprise)** | Businesses, media networks | Webhook API, audit reports, compliance PDFs, SLA dashboard |

Currently the codebase is **neither B2C nor B2B** — it is a monolithic single-controller API with a demo frontend. The separation needs to be designed at the API gateway routing level.

---

## 8. PACKAGE RENAME REQUIRED

**Current package:** `com.antigravity.engine`  
**Required package:** `com.trai.engine`

All 16 Java files need their package declarations updated. The directory structure also needs to change from:
```
src/main/java/com/antigravity/engine/
```
to:
```
src/main/java/com/trai/engine/
```

---

## 9. PRIORITY ORDER — What to Build Next

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| 🔴 P0 | Rename package `antigravity` → `trai` | 30 min | Correctness |
| 🔴 P0 | Add `Dockerfile` for backend | 1 hour | Demo-ability |
| 🔴 P0 | Fix CORS config so frontend can call backend | 30 min | Demo-ability |
| 🔴 P0 | Add `/api/v1/trust/status` health endpoint (currently missing, frontend calls it) | 30 min | Demo-ability |
| 🟠 P1 | Add `i18n` — 3 message files + `LocaleResolver` | 2 hours | Completeness |
| 🟠 P1 | Add webhook catcher service | 1 day | B2B core |
| 🟠 P1 | Add Spring Security + JWT (even basic) | 1 day | Security |
| 🟠 P1 | Add `application-prod.yml` with Yandex vars | 1 hour | Deployment |
| 🟡 P2 | Add input sanitizer + PII masking | 1 day | Security |
| 🟡 P2 | Add unit tests for core services | 1 day | Quality |
| 🟡 P2 | Add Redis caching with `@Cacheable` | 4 hours | Performance |
| 🟢 P3 | Flutter mobile app scaffold | 1 week | Distribution |
| 🟢 P3 | n8n-style pipeline builder UI | 1 week | Product feature |
| 🟢 P3 | Liquibase (if relational DB added) | 2 days | Schema mgmt |

---

## 10. FRONTEND STATUS SUMMARY

**The frontend is a functional demo — not a production application.**

**What works without a backend running:**
- All 4 tabs display correctly
- Live Statement Auditor — works via client-side simulation (returns deterministic mock results)
- AI Glitch Auditor — works via client-side simulation
- News Propaganda Stripper — always uses simulation (no backend call attempted)
- Source Trust Table — shows 6 hardcoded sources

**What requires a running backend:**
- Real AI-powered responses (Grok-2 / Gemini via LangChain4j)
- Persisted source trust scores from MongoDB
- Actual news data from the scraping scheduler

**Verdict:** The frontend is **complete for demo purposes**. It is **not complete** for production use (no auth, no React framework, no mobile support, no real-time WebSocket stream).
