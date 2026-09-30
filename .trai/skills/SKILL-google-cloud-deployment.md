# SKILL: Google Cloud Deployment
# TrAI Platform — Cloud Run, Artifact Registry, API Gateway, GCS, Firebase, CI/CD

## What this skill covers

All Google Cloud infrastructure: how to deploy, configure, and extend the TrAI
production environment on Google Cloud Platform.

---

## Cloud Services Used

| GCP Service | TrAI Usage | Replaces |
|---|---|---|
| **Cloud Run** | Hosts the backend Spring Boot container (stateless, auto-scale) | Yandex Serverless Containers |
| **Artifact Registry** | Stores Docker images (`trai-backend`) | Yandex Container Registry |
| **API Gateway** | Routes HTTPS traffic to Cloud Run | Yandex API Gateway |
| **Cloud Storage (GCS)** | Media/file uploads (`trai-media` bucket) | Yandex Object Storage |
| **Secret Manager** | Stores all secrets (injected via Cloud Run `--update-secrets`) | Yandex Lockbox |
| **Cloud Memorystore** | Managed Redis for caching + rate limiting | Yandex Managed Redis |
| **Vertex AI** | Gemini 2.0 Flash model inference | — |
| **Firebase (FCM)** | Push notifications to Android + iOS | — |

---

## Key Configuration Files

```
TrAI/
├── google-api-gateway.yaml             # Google Cloud API Gateway OpenAPI spec
├── .env.example                        # All required env vars with comments
├── backend/Dockerfile                  # Multi-stage build (JDK 21 → JRE 21 alpine)
├── docker-compose.yml                  # Local dev stack (4 services)
└── .github/workflows/ci.yml            # GitHub Actions CI/CD (5 jobs)
```

---

## Docker Image

### `backend/Dockerfile` structure
```dockerfile
# Stage 1 — Build (JDK 21 + Gradle)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar --no-daemon

# Stage 2 — Runtime (JRE 21 alpine — minimal image)
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S trai && adduser -S trai -G trai  # non-root user
COPY --from=build /app/build/libs/*.jar app.jar
USER trai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Important:** The Dockerfile uses `./gradlew` (Gradle), NOT Maven.
The base image uses `eclipse-temurin:21-jdk-alpine` for build (not `maven:*`).

---

## Docker Compose (Local Dev)

```yaml
services:
  mongodb:   # port 27017 — MongoDB 7.0
  redis:     # port 6379  — Redis 7.2
  trai-backend:   # port 8080 — built from ./backend
  trai-frontend:  # port 3000 → 80 — built from ./frontend
```

Start local: `docker compose up --build`
Backend health: `http://localhost:8080/api/v1/trust/status`
Frontend: `http://localhost:3000`

---

## Google Cloud API Gateway

### Config file: `google-api-gateway.yaml`

Uses `x-google-backend` extension to proxy all `/api/v1/*` requests to the Cloud Run URL.

**To deploy:**
```bash
# 1. Get Cloud Run URL
BACKEND_URL=$(gcloud run services describe trai-backend --region us-central1 --format 'value(status.url)')

# 2. Replace placeholder in spec
sed -i "s|\${BACKEND_SERVICE_URL}|${BACKEND_URL}|g" google-api-gateway.yaml

# 3. Create API config version
gcloud api-gateway api-configs create trai-config-v1 \
    --api=trai-gateway \
    --openapi-spec=google-api-gateway.yaml \
    --project=<YOUR_PROJECT_ID>

# 4. Deploy gateway
gcloud api-gateway gateways create trai-gateway \
    --api=trai-gateway \
    --api-config=trai-config-v1 \
    --location=us-central1 \
    --project=<YOUR_PROJECT_ID>
```

---

## Secret Manager

All secrets stored in Google Secret Manager with names matching the table below.
Cloud Run injects them as environment variables via `--update-secrets`.

| Secret Name in GCP | Env Var in App | Description |
|---|---|---|
| `trai-mongodb-uri` | `MONGODB_URI` | MongoDB Atlas connection string |
| `trai-redis-host` | `REDIS_HOST` | Cloud Memorystore Redis IP |
| `trai-xai-api-key` | `XAI_API_KEY` | xAI Grok API key |
| `trai-openai-api-key` | `OPENAI_API_KEY` | OpenAI ChatGPT API key |
| `trai-twitter-bearer-token` | `TWITTER_BEARER_TOKEN` | Twitter API v2 Bearer Token |
| `trai-gemini-project-id` | `GEMINI_PROJECT_ID` | GCP project ID for Vertex AI |
| `trai-jwt-secret` | `JWT_SECRET` | JWT signing secret (min 64 chars) |
| `trai-fcm-server-key` | `FCM_SERVER_KEY` | Firebase server key |

**To add a new secret:**
```bash
echo -n "secret-value" | gcloud secrets create <secret-name> --data-file=-
```

Then add it to the `--update-secrets` flag in `.github/workflows/ci.yml` deploy step.

---

## CI/CD Pipeline — `.github/workflows/ci.yml`

### Jobs (run in parallel unless stated)

| Job | Trigger | Description |
|---|---|---|
| `backend-build-and-test` | All branches | JDK 21, `./gradlew clean check bootJar` |
| `frontend-validation` | All branches | Verify static files exist, Docker build check |
| `docker-compose-validate` | All branches | `docker compose config -q` |
| `flutter-build-check` | All branches | `flutter pub get && flutter analyze && flutter test` |
| `deploy-to-google-cloud` | `push` to `main` only | Build + push image → Cloud Run deploy |

### GitHub Secrets Required

| Secret | Value |
|---|---|
| `GCP_SA_KEY` | Service account JSON key (base64) |
| `GCP_PROJECT_ID` | Your GCP project ID |
| `GCP_REGION` | e.g. `us-central1` |
| `GCP_ARTIFACT_REGISTRY` | e.g. `us-central1-docker.pkg.dev` |
| `GCP_CLOUD_RUN_SERVICE` | e.g. `trai-backend` |

### Cloud Run deploy flags
```bash
gcloud run deploy trai-backend \
    --image <IMAGE> \
    --memory 1Gi \
    --cpu 2 \
    --concurrency 80 \
    --min-instances 0 \     # Scale to zero when idle (cost saving)
    --max-instances 10 \
    --port 8080 \
    --allow-unauthenticated  # API Gateway handles auth
```

---

## Google Cloud Storage (GCS)

Used for media/file uploads. Replaces Yandex Object Storage.

### Config
```yaml
gcs:
  bucket-name: ${GCS_BUCKET_NAME:trai-media}
  project-id: ${GCS_PROJECT_ID:your-project-id}
```

### Authentication
Uses `GOOGLE_APPLICATION_CREDENTIALS` env var (service account JSON).
On Cloud Run, this is handled automatically by the service account attached to the instance.

### Dependency
```gradle
implementation 'com.google.cloud:google-cloud-storage:2.40.1'
```

---

## Adding a New GCP Service

1. Enable the API in Google Cloud Console
2. Grant service account the required role (IAM)
3. Add client library to `backend/build.gradle`
4. Add config to `application.yml` and `application-prod.yml`
5. Add secret to GCP Secret Manager + add to Cloud Run deploy `--update-secrets`
6. Add env var to `.env.example` with clear comment
7. Update this skill file with the new service entry

---

## Playwright Test Coverage Trigger

After any CI/CD or deployment config change:

- `tests/infrastructure/health.spec.ts` — hit deployed Cloud Run URL's `/api/v1/trust/status`
  and assert `"status": "OPERATIONAL"`
- These tests run as a post-deploy smoke test job (can be added to `ci.yml` as a 6th job)
