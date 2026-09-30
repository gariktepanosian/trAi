# TrAI GATEWAY
# Master Skill Router for AI Agents

## PURPOSE

You are an AI agent working on the TrAI platform.
**Read this file first — before reading any code.**

This GATEWAY file maps any development request to the exact skill file(s) you must
read before writing a single line of code. Each skill file contains the project's
conventions, patterns, file locations, and Playwright test requirements for that area.

---

## HOW TO USE THIS GATEWAY

1. Read your task/request
2. Find matching keywords in the routing table below
3. Read the listed skill file(s) completely before writing code
4. After writing code, check the skill file's **"Playwright Test Coverage Trigger"** section
5. Write or update the required Playwright tests
6. If you added a new subsystem, add a new `SKILL-<name>.md` file and add it to this GATEWAY

---

## ROUTING TABLE

### AI / Machine Learning

| If your task involves... | Read this skill file |
|---|---|
| Adding a new AI model (Anthropic, Llama, Mistral...) | `SKILL-ai-engine.md` |
| Changing the Grok model version | `SKILL-ai-engine.md` |
| Changing the Gemini model version | `SKILL-ai-engine.md` |
| Changing the ChatGPT/OpenAI model version | `SKILL-ai-engine.md` |
| Adding a new `@SystemMessage` AI interface | `SKILL-ai-engine.md` |
| Wiring an AI interface bean in `AiConfig.java` | `SKILL-ai-engine.md` |
| Changing a prompt or system message | `SKILL-ai-engine.md` |
| Adding a new LangChain4j feature | `SKILL-ai-engine.md` |
| Fallback heuristics for AI unavailability | `SKILL-ai-engine.md` |
| AI JSON output parsing | `SKILL-ai-engine.md` |

### Multi-Agent Pipeline

| If your task involves... | Read this skill file |
|---|---|
| Adding a third agent to the pipeline | `SKILL-multi-agent-pipeline.md` |
| Changing how agents run in parallel | `SKILL-multi-agent-pipeline.md` |
| Changing the verdict scale (score thresholds) | `SKILL-multi-agent-pipeline.md` |
| Changing critical alert detection keywords | `SKILL-multi-agent-pipeline.md` + `SKILL-twitter-country-monitoring.md` |
| Changing `MultiAgentResult` fields | `SKILL-multi-agent-pipeline.md` |
| Changing `AgentOutput` fields | `SKILL-multi-agent-pipeline.md` |
| The `POST /api/v1/validate/multi-agent` endpoint | `SKILL-multi-agent-pipeline.md` |
| Timeout handling for parallel AI calls | `SKILL-multi-agent-pipeline.md` |
| Java virtual threads / executor | `SKILL-multi-agent-pipeline.md` |

### Twitter / X Monitoring & User Authority

| If your task involves... | Read this skill file |
|---|---|
| Adding a new critical event keyword | `SKILL-twitter-country-monitoring.md` |
| Changing the X API search query | `SKILL-twitter-country-monitoring.md` |
| Changing the virality weight formula | `SKILL-twitter-country-monitoring.md` |
| Adding a new country to monitor | `SKILL-twitter-country-monitoring.md` |
| Changing how posts are aggregated | `SKILL-twitter-country-monitoring.md` |
| Changing the monitoring schedule (10 min) | `SKILL-twitter-country-monitoring.md` |
| Moving subscription storage to MongoDB | `SKILL-twitter-country-monitoring.md` |
| `CountryMonitoringService` changes | `SKILL-twitter-country-monitoring.md` |
| `TwitterApiService` changes | `SKILL-twitter-country-monitoring.md` |
| `/api/v1/country/*` endpoints | `SKILL-twitter-country-monitoring.md` |
| X user authority ranking | `SKILL-twitter-country-monitoring.md` + `SKILL-ai-engine.md` |
| `XUserRankingService` changes | `SKILL-twitter-country-monitoring.md` + `SKILL-ai-engine.md` |
| `XUserRankScore` MongoDB document | `SKILL-twitter-country-monitoring.md` + `SKILL-backend-architecture.md` |
| `x_user_rank_scores` collection | `SKILL-twitter-country-monitoring.md` |
| Source authority ranking | `SKILL-ai-engine.md` + `SKILL-backend-architecture.md` |
| `SourceAuthorityService` changes | `SKILL-ai-engine.md` + `SKILL-backend-architecture.md` |

### Push Notifications

| If your task involves... | Read this skill file |
|---|---|
| Firebase / FCM setup | `SKILL-push-notifications.md` |
| Android push notification config | `SKILL-push-notifications.md` |
| iOS APNs push notification config | `SKILL-push-notifications.md` |
| Adding a new notification type | `SKILL-push-notifications.md` |
| Changing when push notifications fire | `SKILL-push-notifications.md` + `SKILL-multi-agent-pipeline.md` |
| Topic-based notifications | `SKILL-push-notifications.md` |
| `PushNotificationService` changes | `SKILL-push-notifications.md` |
| `notification_service.dart` (Flutter) | `SKILL-push-notifications.md` + `SKILL-flutter-mobile.md` |
| `FCM_SERVER_KEY` / `FIREBASE_CREDENTIALS_PATH` | `SKILL-push-notifications.md` |

### Flutter Mobile App

| If your task involves... | Read this skill file |
|---|---|
| Adding a new Flutter screen | `SKILL-flutter-mobile.md` |
| Adding a new Flutter tab | `SKILL-flutter-mobile.md` |
| Adding a new Flutter service | `SKILL-flutter-mobile.md` |
| State management (`CountryService`, `ChangeNotifier`) | `SKILL-flutter-mobile.md` |
| Navigation changes (`IndexedStack`, `Navigator`) | `SKILL-flutter-mobile.md` |
| Adding a new `pubspec.yaml` dependency | `SKILL-flutter-mobile.md` |
| `ApiService` HTTP methods | `SKILL-flutter-mobile.md` |
| Country picker integration | `SKILL-flutter-mobile.md` + `SKILL-twitter-country-monitoring.md` |
| Colors / theming in Flutter | `SKILL-flutter-mobile.md` |
| `SharedPreferences` usage | `SKILL-flutter-mobile.md` |
| `mounted` check / setState safety | `SKILL-flutter-mobile.md` |

### Backend Architecture

| If your task involves... | Read this skill file |
|---|---|
| Adding a new Java package | `SKILL-backend-architecture.md` |
| Adding a new `@RestController` | `SKILL-backend-architecture.md` |
| Adding a new `@Service` | `SKILL-backend-architecture.md` |
| Adding a new MongoDB `@Document` | `SKILL-backend-architecture.md` |
| Adding a new `MongoRepository` | `SKILL-backend-architecture.md` |
| Adding a new DTO | `SKILL-backend-architecture.md` |
| Naming conventions (class, method, endpoint) | `SKILL-backend-architecture.md` |
| Caching with `@Cacheable` | `SKILL-backend-architecture.md` |
| Error handling / fallback pattern | `SKILL-backend-architecture.md` |
| `application.yml` / `application-prod.yml` changes | `SKILL-backend-architecture.md` |
| Adding a new env var | `SKILL-backend-architecture.md` + `.env.example` |

### Security & Authentication

| If your task involves... | Read this skill file |
|---|---|
| JWT authentication | `SKILL-security-auth.md` |
| Making an endpoint public or protected | `SKILL-security-auth.md` |
| Rate limiting changes | `SKILL-security-auth.md` |
| Input sanitizer changes | `SKILL-security-auth.md` |
| Output guardrails changes | `SKILL-security-auth.md` |
| CORS configuration | `SKILL-security-auth.md` |
| Audit logging | `SKILL-security-auth.md` |
| Password / BCrypt | `SKILL-security-auth.md` |
| `SecurityConfig.java` changes | `SKILL-security-auth.md` |

### Google Cloud & Deployment

| If your task involves... | Read this skill file |
|---|---|
| Deploying to Google Cloud Run | `SKILL-google-cloud-deployment.md` |
| Google Cloud API Gateway | `SKILL-google-cloud-deployment.md` |
| Artifact Registry / Docker images | `SKILL-google-cloud-deployment.md` |
| Google Cloud Storage (GCS) | `SKILL-google-cloud-deployment.md` |
| Secret Manager | `SKILL-google-cloud-deployment.md` |
| CI/CD pipeline (`ci.yml`) changes | `SKILL-google-cloud-deployment.md` |
| Adding a new GCP service | `SKILL-google-cloud-deployment.md` |
| Docker / Dockerfile changes | `SKILL-google-cloud-deployment.md` |
| `docker-compose.yml` changes | `SKILL-google-cloud-deployment.md` |
| GitHub Actions secrets | `SKILL-google-cloud-deployment.md` |

### Testing

| If your task involves... | Read this skill file |
|---|---|
| Writing a Playwright test | `SKILL-playwright-testing.md` |
| Adding a new API test | `SKILL-playwright-testing.md` |
| Adding a new UI test | `SKILL-playwright-testing.md` |
| Mocking AI responses in tests | `SKILL-playwright-testing.md` |
| `playwright.config.ts` changes | `SKILL-playwright-testing.md` |
| `tests/helpers/*` changes | `SKILL-playwright-testing.md` |
| Test structure / naming | `SKILL-playwright-testing.md` |
| Running tests in CI | `SKILL-playwright-testing.md` + `SKILL-google-cloud-deployment.md` |

---

## COMPOUND TASKS (multiple skills required)

Some tasks span multiple subsystems. Read ALL listed skill files:

| Task | Skill files to read |
|---|---|
| **Add a new AI model to the pipeline** | `SKILL-ai-engine.md` + `SKILL-multi-agent-pipeline.md` |
| **Add a new country event type** | `SKILL-twitter-country-monitoring.md` + `SKILL-push-notifications.md` + `SKILL-multi-agent-pipeline.md` |
| **Add a new Flutter screen with backend API** | `SKILL-flutter-mobile.md` + `SKILL-backend-architecture.md` + `SKILL-playwright-testing.md` |
| **Add a new push notification trigger** | `SKILL-push-notifications.md` + `SKILL-multi-agent-pipeline.md` + `SKILL-flutter-mobile.md` |
| **Deploy a new backend feature to production** | `SKILL-backend-architecture.md` + `SKILL-google-cloud-deployment.md` + `SKILL-playwright-testing.md` |
| **Change the critical alert threshold** | `SKILL-multi-agent-pipeline.md` + `SKILL-twitter-country-monitoring.md` + `SKILL-push-notifications.md` |
| **Add user authentication to a mobile screen** | `SKILL-flutter-mobile.md` + `SKILL-security-auth.md` |
| **Extend X user authority ranking** | `SKILL-twitter-country-monitoring.md` + `SKILL-ai-engine.md` + `SKILL-backend-architecture.md` |
| **Extend source authority / trust scoring** | `SKILL-ai-engine.md` + `SKILL-backend-architecture.md` |

---

## AFTER EVERY CODE CHANGE — MANDATORY CHECKLIST

Before considering any task complete, the AI agent MUST verify:

- [ ] Read the relevant skill file(s) listed above
- [ ] Followed naming conventions from `SKILL-backend-architecture.md`
- [ ] Added env vars to `.env.example` if new keys were introduced
- [ ] Added config to `application.yml` AND `application-prod.yml` if new config was added
- [ ] Checked the skill file's **"Playwright Test Coverage Trigger"** section
- [ ] Created or updated Playwright tests for any changed endpoint or UI
- [ ] Verified fallback behavior works when AI API keys are not set
- [ ] No hardcoded secrets, API keys, or `localhost` URLs in production code

---

## WHEN TO ADD A NEW SKILL FILE

Create `SKILL-<name>.md` when you add a subsystem that does not match any existing skill.
Then add it to this GATEWAY routing table.

Trigger examples:
- Adding Elasticsearch integration → `SKILL-search.md`
- Adding WebSocket streaming → `SKILL-realtime-streaming.md`
- Adding a payment system → `SKILL-billing.md`
- Adding Anthropic Claude as a model → update `SKILL-ai-engine.md` (no new file needed)

---

## SKILL FILE TEMPLATE

When creating a new `SKILL-<name>.md`, use this structure:

```markdown
# SKILL: <Subsystem Name>
# TrAI Platform — <Brief description>

## What this skill covers
<One paragraph explaining what this subsystem does and why it exists>

## File Locations
<List all relevant files with full paths>

## Architecture
<Diagram or description of how the subsystem works>

## Patterns / Conventions
<How to write code for this subsystem — naming, structure, error handling>

## Key Data Structures
<List important classes/types with their fields>

## Configuration
<env vars, yml config, build.gradle deps>

## Common Mistakes
<List of things NOT to do>

## Playwright Test Coverage Trigger
<Which test files must be updated/created after changes to this subsystem>
```

---

## PROJECT OVERVIEW (Quick Reference)

```
TrAI Platform — Truth Infrastructure
├── Backend:  Spring Boot 4.0.3 / Java 21 / MongoDB / Redis
│             REST API at /api/v1/*
│             Deployed to: Google Cloud Run
│
├── AI:       Grok-4.7 (xAI) — PRIMARY
│             Gemini 2.5 Flash (Google Vertex AI) — SECONDARY
│             GPT-4o (OpenAI) — TERTIARY / FILTER
│             Pipeline: Dual-Agent × 3 models = 6 parallel calls
│             X User Ranking: Grok + Gemini per author (XUserRankingService)
│             Source Authority: Grok + Gemini per source (SourceAuthorityService)
│
├── Data:     X (formerly Twitter) API v2 — real posts per country
│             MongoDB — news, alerts, users, audit logs,
│                       x_user_rank_scores, source_trust_scores
│             Redis — caching (10 min TTL)
│
├── Mobile:   Flutter (iOS + Android)
│             4 tabs: Live Check | News | Alerts | Settings
│             Push notifications via FCM (Android + iOS APNs bridge)
│
├── Frontend: Vanilla HTML/CSS/JS SPA (9-tab web dashboard)
│
├── Cloud:    Google Cloud Run, Artifact Registry, API Gateway
│             GCS (media), Secret Manager, Memorystore (Redis)
│             Firebase FCM (push notifications)
│
└── Tests:    Playwright — tests/api/ (API) + tests/ui/ (UI)
              Flutter widget tests — mobile/test/
              Spring unit tests — backend/src/test/
```
