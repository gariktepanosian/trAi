# TrAI — Enterprise Truth Infrastructure & Real-Time Fact-Checking Platform

> **Trust-as-a-Service (TaaS) Engine for Media, Finance, and Enterprise AI Guardrails**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.29.1-blue.svg)](https://github.com/langchain4j/langchain4j)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%207.0-green.svg)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis%207.2-red.svg)](https://redis.io/)
[![AI Engines](https://img.shields.io/badge/AI-xAI%20Grok--2%20%7C%20Gemini%201.5%20Pro-purple.svg)](https://x.ai)
[![CI](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-black.svg)](https://github.com/features/actions)
[![Docker](https://img.shields.io/badge/Docker-Compose%20v3.8-blue.svg)](https://docs.docker.com/compose/)

---

## Table of Contents

1. [What Is TrAI](#1-what-is-trai)
2. [Architecture Overview](#2-architecture-overview)
3. [Project Structure](#3-project-structure)
4. [Prerequisites](#4-prerequisites)
5. [Environment Variables](#5-environment-variables)
6. [Running the Application](#6-running-the-application)
   - [Option A — Demo Mode (No Backend Required)](#option-a--demo-mode-no-backend-required)
   - [Option B — Full Stack Local Development](#option-b--full-stack-local-development)
   - [Option C — Full Docker Compose Stack](#option-c--full-docker-compose-stack)
7. [Running the Mobile App](#7-running-the-mobile-app)
8. [Running Tests](#8-running-tests)
9. [Building for Production](#9-building-for-production)
10. [CI/CD Pipeline](#10-cicd-pipeline)
11. [Cloud Deployment — Yandex Cloud](#11-cloud-deployment--yandex-cloud)
12. [API Reference](#12-api-reference)
13. [Database Schema](#13-database-schema)
14. [Security Architecture](#14-security-architecture)
15. [Internationalization](#15-internationalization)
16. [Troubleshooting](#16-troubleshooting)
17. [Contributing](#17-contributing)

---

## 1. What Is TrAI

**TrAI** is a production-ready **Trust-as-a-Service (TaaS)** platform. It acts as an autonomous verification gateway that sits between raw incoming information and downstream consumers — journalists, businesses, analysts, and AI systems.

**Three core AI capabilities:**

| Capability | Description | Endpoint |
|---|---|---|
| **Live Speech Fact-Checking** | Verifies transcribed spoken claims from live speeches, debates, or broadcasts in real time | `POST /api/v1/live/verify-statement` |
| **AI Hallucination Auditor** | Detects fabricated citations, invented statistics, and logical errors in LLM outputs | `POST /api/v1/trust/verify-ai-output` |
| **News Propaganda Stripping** | Normalizes biased articles to neutral, factual language using radical objectivity | `POST /api/v1/trust/verify-news` |

**Additional platform features:**

- Predictive market impact analytics (Gold, BTC, Oil, USD)
- B2B webhook gateway with HMAC-SHA256 signature verification
- Global source credibility index and trust scoring
- SentinelMind emergency kill switch
- Incident and alert center
- Multi-language truth feeds (English, Russian, Armenian)
- Full-text news archive search
- JWT authentication and role-based access control
- Complete audit logging for compliance reporting
- Automated news scraping scheduler (every 10 minutes)

---

## 2. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            CLIENT LAYER                                   │
│   Web Browser (index.html)    Flutter Mobile App    B2B Partner Systems  │
└────────────┬──────────────────────────┬──────────────────────┬───────────┘
             │                          │                      │ HMAC-SHA256
             ▼                          ▼                      ▼
┌──────────────────────────────────────────────────────────────────────────┐
│              REST API LAYER  (Spring Boot 4.0.3 / Java 21)               │
│  /api/v1/live/*   /api/v1/trust/*   /api/v1/analytics/*                  │
│  /api/v1/webhook/*  /api/v1/auth/*  /api/v1/translate/*                  │
│  /api/v1/search/*   /api/v1/alerts/*  /api/v1/audit/*                    │
└────────────┬─────────────────────────────────────────────────────────────┘
             │  RateLimitingFilter → JwtAuthFilter → InputSanitizerService
             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│               SERVICE LAYER  (TrustVerificationService)                   │
│  Kill Switch → Sanitize → AI Call → Fallback → Guardrails → Audit → Cache│
└──────┬────────────────┬──────────────────────┬────────────────────────────┘
       ▼                ▼                      ▼
┌─────────────┐  ┌──────────────┐   ┌──────────────────┐
│ LangChain4j │  │  MongoDB 7.0 │   │   Redis 7.2       │
│ AI Engines  │  │  Documents + │   │  Cache (10 min    │
│ ─────────── │  │  Users +     │   │  TTL) for news,   │
│ xAI Grok-2  │  │  Audit Logs +│   │  trust scores,    │
│ Gemini 1.5  │  │  Webhooks +  │   │  analytics,       │
│             │  │  Alerts      │   │  translations     │
└─────────────┘  └──────────────┘   └──────────────────┘
```

**Request pipeline (every API call):**

1. `RateLimitingFilter` — per-IP token bucket, 120 requests/minute (Bucket4j)
2. `JwtAuthFilter` — validates JWT, populates Spring Security context
3. Controller routes to `TrustVerificationService`
4. `InputSanitizerService` — strips HTML, blocks SQL injection (4 patterns), blocks prompt injection (9 patterns), masks PII
5. Kill switch check — if `SentinelMind` active, returns `HALTED` immediately
6. LangChain4j AI engine invoked (xAI Grok-2 via OpenAI-compatible endpoint)
7. Heuristic fallback engine triggered automatically if AI call fails (no external dependency)
8. `OutputGuardrailsService` — blocks toxic content, redacts PII leakage, blocks risk score ≥ 85
9. `AuditLogService` — persists every call with actor, verdict, trust score, latency to MongoDB
10. Redis cache applied for repeated identical inputs

---

## 3. Project Structure

```
Anti-gravity AI/                         ← Git repository root
│
├── .env.example                         ← All required environment variables (template)
├── .gitignore
├── .github/
│   └── workflows/
│       └── ci.yml                       ← GitHub Actions CI/CD pipeline
│
├── build.gradle                         ← Root Gradle build
├── settings.gradle
├── gradlew / gradlew.bat                ← Gradle wrappers
│
├── docker-compose.yml                   ← Full 4-service stack (MongoDB + Redis + Backend + Frontend)
├── yandex-api-gateway.yaml             ← Yandex Cloud API Gateway OpenAPI spec
│
├── README.md                            ← This file (developer guide)
├── BUSINESS_PRESENTATION.md            ← B2B sales pitch deck (12 slides)
├── USER_PRESENTATION.md                ← End-user product guide
├── GAPS_AND_STATUS.md                  ← Feature audit (100% complete)
├── RUNNING_GUIDE.md                    ← Legacy quick-start reference
│
├── backend/                             ← Spring Boot 4.0.3 / Java 21 application
│   ├── Dockerfile                       ← Multi-stage Docker build
│   ├── build.gradle
│   ├── gradlew / gradlew.bat
│   └── src/
│       ├── main/java/com/trai/engine/
│       │   ├── ai/                      ← LangChain4j AI engine interfaces
│       │   │   ├── AntiPropagandaEngine.java
│       │   │   ├── LiveFactCheckEngine.java
│       │   │   └── AiGlitchVerifierEngine.java
│       │   ├── analytics/
│       │   │   ├── MarketImpactEngine.java
│       │   │   └── PredictiveAnalyticsService.java
│       │   ├── alert/
│       │   │   ├── PlatformAlert.java
│       │   │   ├── PlatformAlertRepository.java
│       │   │   └── PlatformAlertService.java
│       │   ├── audit/
│       │   │   ├── AuditLog.java
│       │   │   ├── AuditLogRepository.java
│       │   │   ├── AuditLogService.java
│       │   │   └── AuditController.java
│       │   ├── config/
│       │   │   ├── AiConfig.java        ← Grok-2 + Gemini LangChain4j wiring
│       │   │   ├── I18nConfig.java
│       │   │   └── RedisConfig.java
│       │   ├── controller/
│       │   │   ├── TrustEngineController.java   ← Main REST API
│       │   │   ├── AuthController.java
│       │   │   ├── AlertController.java
│       │   │   ├── NewsSearchController.java
│       │   │   └── TranslationController.java
│       │   ├── domain/                  ← MongoDB document models
│       │   │   ├── NormalizedNews.java
│       │   │   ├── SourceTrustScore.java
│       │   │   ├── InsiderInfo.java
│       │   │   ├── VerifiedClaim.java
│       │   │   └── TraiUser.java
│       │   ├── dto/                     ← Request/Response DTOs
│       │   ├── guardrail/
│       │   │   └── OutputGuardrailsService.java
│       │   ├── repository/              ← Spring Data MongoDB repositories
│       │   ├── sanitizer/
│       │   │   └── InputSanitizerService.java
│       │   ├── scheduler/
│       │   │   ├── NewsScrapingScheduler.java            ← Every 10 min
│       │   │   └── TrustScoreRecalculationScheduler.java ← Daily
│       │   ├── search/
│       │   │   ├── NewsSearchDocument.java
│       │   │   └── NewsSearchService.java
│       │   ├── security/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtUtil.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   ├── TraiUserDetailsService.java
│       │   │   └── RateLimitingFilter.java
│       │   ├── service/
│       │   │   └── TrustVerificationService.java   ← Core orchestrator
│       │   ├── translation/
│       │   │   └── TranslationService.java
│       │   └── webhook/
│       │       ├── WebhookController.java
│       │       ├── WebhookService.java  ← HMAC-SHA256 B2B gateway
│       │       ├── WebhookPartner.java
│       │       └── WebhookRequest.java
│       ├── main/resources/
│       │   ├── application.yml          ← Local dev config
│       │   ├── application-prod.yml     ← Production config
│       │   ├── messages.properties      ← i18n English
│       │   ├── messages_ru.properties   ← i18n Russian
│       │   ├── messages_hy.properties   ← i18n Armenian
│       │   └── db/changelog/           ← Liquibase migrations
│       │       ├── db.changelog-master.yaml
│       │       ├── changes/001-initial-schema.sql
│       │       └── changes/002-source-indices.sql
│       └── test/java/com/trai/engine/  ← Unit test suites
│
├── frontend/                            ← Web SPA (Vanilla HTML/CSS/JS)
│   ├── index.html                       ← 9-tab single-page application
│   ├── style.css                        ← Dark glassmorphism theme
│   ├── app.js                           ← Frontend logic (~1100 lines)
│   └── Dockerfile                       ← Nginx 1.27-alpine container
│
└── mobile/                              ← Flutter cross-platform app
    ├── pubspec.yaml
    └── lib/
        ├── main.dart
        ├── screens/
        │   ├── home_screen.dart
        │   └── live_fact_check_screen.dart
        └── services/
            └── api_service.dart
```

---

## 4. Prerequisites

### Required tools

| Tool | Version | Download |
|---|---|---|
| **Java JDK** | 21 LTS (Temurin recommended) | https://adoptium.net |
| **Docker Desktop** | Latest (Engine 24+, Compose v2+) | https://docker.com/products/docker-desktop |
| **Git** | Latest | https://git-scm.com |

### Optional tools

| Tool | Purpose |
|---|---|
| **IntelliJ IDEA 2024.x+** | Java IDE — highly recommended |
| **Gradle 8.x** | Included via wrapper (`./gradlew`), no separate install needed |
| **Flutter SDK 3.x** | Required only for running the mobile app |
| **Node.js 20 LTS** | Only needed if converting frontend to React |

### API Keys

| Key | Where to get | Required for |
|---|---|---|
| `XAI_API_KEY` | https://console.x.ai | xAI Grok-2 — primary AI engine |
| `GEMINI_PROJECT_ID` | Google Cloud Console | Vertex AI Gemini — secondary AI model |
| `GEMINI_LOCATION` | Google Cloud Console | Vertex AI region (e.g. `us-central1`) |

> **API keys are optional for demo mode.** The frontend runs in full simulation mode without any keys, and the backend uses a built-in heuristic fallback engine when AI calls fail.

---

## 5. Environment Variables

Copy `.env.example` to `.env` and fill in your values. **Never commit `.env` to git.**

```env
# Database — MongoDB
MONGODB_URI=mongodb://localhost:27017/trai

# Cache — Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# AI Engines
XAI_API_KEY=xai_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
GEMINI_PROJECT_ID=your-google-cloud-project-id
GEMINI_LOCATION=us-central1

# Authentication
JWT_SECRET=change-this-to-a-very-long-random-string-at-least-256-bits
JWT_EXPIRATION_MS=86400000

# Yandex Object Storage (production only)
YANDEX_STORAGE_KEY=your-yandex-storage-access-key
YANDEX_STORAGE_SECRET=your-yandex-storage-secret-key
YANDEX_STORAGE_BUCKET=trai-assets

# B2B Webhook
WEBHOOK_SIGNING_SECRET=whsec_change-this-before-going-to-production
```

### Setting variables per OS

**Windows PowerShell:**
```powershell
$env:XAI_API_KEY = "xai-your-key-here"
$env:GEMINI_PROJECT_ID = "your-gcp-project-id"
$env:JWT_SECRET = "your-very-long-secret-string"
```

**Windows Command Prompt:**
```cmd
set XAI_API_KEY=xai-your-key-here
set GEMINI_PROJECT_ID=your-gcp-project-id
set JWT_SECRET=your-very-long-secret-string
```

**macOS / Linux (Bash/Zsh):**
```bash
export XAI_API_KEY="xai-your-key-here"
export GEMINI_PROJECT_ID="your-gcp-project-id"
export JWT_SECRET="your-very-long-secret-string"
```

---

## 6. Running the Application

### Option A — Demo Mode (No Backend Required)

The fastest way to see the full UI. No Java, no Docker, no API keys needed.

```
1. Open the frontend/ folder
2. Double-click index.html (Windows) or run: open frontend/index.html (macOS)
3. The header shows: "Backend: Standalone Demo Mode (Active)"
4. All 9 tabs are fully functional with simulated responses
```

Use this for client demonstrations and investor presentations.

---

### Option B — Full Stack Local Development

**Step 1 — Start infrastructure (MongoDB + Redis):**

```bash
docker-compose up -d mongodb redis
```

Verify containers are healthy:
```bash
docker ps
```

You should see `trai-mongo` running on port `27017` and `trai-redis` on port `6379`.

**Step 2 — Set environment variables** (see [Section 5](#5-environment-variables)):

```powershell
# Windows PowerShell example:
$env:XAI_API_KEY = "your-key"
$env:JWT_SECRET = "your-secret"
```

**Step 3 — Build and start the backend:**

Windows:
```powershell
cd backend
.\gradlew.bat bootRun
```

macOS / Linux:
```bash
cd backend
chmod +x gradlew
./gradlew bootRun
```

Wait for the Spring Boot startup banner:
```
Started TrAIEngine in X.XXX seconds (JVM running for X.XXX)
Tomcat started on port(s): 8080 (http)
```

**Step 4 — Verify backend health:**

```bash
curl http://localhost:8080/api/v1/trust/status
```

Expected response:
```json
{
  "application": "TrAI Trust Engine Platform",
  "version": "0.0.1-SNAPSHOT",
  "status": "OPERATIONAL",
  "features": [
    "Live Video/Speech Transcription Fact-Checking",
    "AI Hallucination & Glitch Detection",
    "Multi-source Consensus Verification",
    "Propaganda & Double-Standard Stripping",
    "Source Trust Scoring Index"
  ]
}
```

**Step 5 — Open the frontend:**

Open `frontend/index.html` in any modern browser. The header badge will turn green:
```
Backend: OPERATIONAL (v0.0.1-SNAPSHOT)
```

**Step 6 — Register a user (optional, enables authenticated requests):**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "developer", "email": "dev@example.com", "password": "StrongPass123!"}'
```

---

### Option C — Full Docker Compose Stack

This runs all 4 services (MongoDB, Redis, Backend, Frontend) in containers.

**Step 1 — Build the backend JAR:**

Windows:
```powershell
cd backend
.\gradlew.bat clean bootJar -x test
cd ..
```

macOS / Linux:
```bash
cd backend && ./gradlew clean bootJar -x test && cd ..
```

**Step 2 — Configure environment:**

Create a `.env` file in the project root by copying `.env.example` and filling in your values.

**Step 3 — Build and start all containers:**

```bash
docker-compose up -d --build
```

**Step 4 — Verify all services are healthy:**

```bash
docker-compose ps
```

All four services should show status `healthy` or `running`:

| Service | Port | Purpose |
|---|---|---|
| `trai-backend` | `8080` | Spring Boot API server |
| `trai-frontend` | `3000` | Nginx static file server |
| `mongodb` | `27017` | MongoDB 7.0 document store |
| `redis` | `6379` | Redis 7.2 cache layer |

**Step 5 — Access the application:**

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1/trust/status
- Backend actuator health: http://localhost:8080/actuator/health

**Useful Docker Compose commands:**

```bash
# Watch real-time logs from backend
docker-compose logs -f trai-backend

# Watch all service logs
docker-compose logs -f

# Stop all services (keep data)
docker-compose down

# Stop and wipe all volumes (full reset)
docker-compose down -v

# Restart only the backend after a code change
docker-compose up -d --build trai-backend

# Scale backend to 3 replicas (if using external load balancer)
docker-compose up -d --scale trai-backend=3
```

---

## 7. Running the Mobile App

The Flutter mobile app targets iOS, Android, and Chrome (web).

**Prerequisites:** Flutter SDK 3.x installed. Run `flutter doctor` to verify your setup.

```bash
# Install dependencies
cd mobile
flutter pub get

# Run in Chrome (web — fastest for development)
flutter run -d chrome

# Run on Android emulator or connected device
flutter run -d android

# Run on iOS simulator (macOS only)
flutter run -d ios
```

The app connects to `http://10.0.2.2:8080` (Android emulator address for `localhost`). For physical devices or iOS, update the base URL in `mobile/lib/services/api_service.dart`.

---

## 8. Running Tests

### Backend unit tests

Run all 5 test suites (TrustVerificationService, InputSanitizer, WebhookService, TranslationService, NewsSearchService):

Windows:
```powershell
cd backend
.\gradlew.bat check
```

macOS / Linux:
```bash
cd backend && ./gradlew check
```

Run tests and produce a coverage report:
```bash
./gradlew check jacocoTestReport
```

Test reports are written to:
```
backend/build/reports/tests/test/index.html
```

### Run a specific test class

```bash
./gradlew test --tests "com.trai.engine.service.TrustVerificationServiceTest"
./gradlew test --tests "com.trai.engine.sanitizer.InputSanitizerServiceTest"
./gradlew test --tests "com.trai.engine.webhook.WebhookServiceTest"
```

### Frontend validation

The CI pipeline validates frontend static assets automatically. To run locally:

```bash
test -f frontend/index.html && test -f frontend/style.css && test -f frontend/app.js && echo "OK"
```

---

## 9. Building for Production

### Build the backend JAR

Windows:
```powershell
cd backend
.\gradlew.bat clean check bootJar
```

macOS / Linux:
```bash
cd backend && ./gradlew clean check bootJar
```

The production JAR is written to: `backend/build/libs/backend-0.0.1-SNAPSHOT.jar`

### Build the backend Docker image

```bash
# Build JAR first (see above), then:
docker build -t trai-backend:latest ./backend
```

### Run the JAR directly (without Docker)

```bash
java \
  -Dspring.profiles.active=prod \
  -DXAI_API_KEY=your-key \
  -DJWT_SECRET=your-secret \
  -DMONGODB_URI=mongodb://your-host:27017/trai \
  -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

### Production Spring profile

The `application-prod.yml` profile enables:

- GZIP compression for all responses
- Stack traces hidden from error responses
- Actuator exposes only `health` and `info` endpoints
- Logging: root=WARN, com.trai=INFO
- Redis TTL set to 10 minutes

Activate with: `--spring.profiles.active=prod`

---

## 10. CI/CD Pipeline

The GitHub Actions pipeline (`.github/workflows/ci.yml`) runs automatically on every push and pull request to `main`.

### Pipeline jobs

| Job | Runner | What it does |
|---|---|---|
| `backend-build-and-test` | `ubuntu-latest` | Sets up JDK 21 (Temurin), runs `./gradlew clean check bootJar --no-daemon`, uploads test reports as artifacts |
| `frontend-validation` | `ubuntu-latest` | Verifies `index.html`, `style.css`, `app.js` exist, then builds `trai-frontend:ci` Docker image |
| `docker-compose-validate` | `ubuntu-latest` | Runs `docker compose config -q` to validate `docker-compose.yml` syntax |

All three jobs run in parallel.

### Viewing pipeline results

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select the most recent workflow run
4. Download test reports from **Artifacts**: `backend-test-results`

### Extending the pipeline

To add a deployment step to Yandex Cloud, append to `ci.yml` after the build job:

```yaml
deploy-to-yandex:
  name: Deploy to Yandex Cloud
  needs: [backend-build-and-test, frontend-validation]
  runs-on: ubuntu-latest
  if: github.ref == 'refs/heads/main' && github.event_name == 'push'

  steps:
    - uses: actions/checkout@v4

    - name: Install Yandex CLI
      run: curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash

    - name: Authenticate with Yandex Cloud
      run: |
        echo "${{ secrets.YC_SA_JSON_CREDENTIALS }}" > sa-key.json
        yc config set service-account-key sa-key.json
        yc config set cloud-id ${{ secrets.YC_CLOUD_ID }}
        yc config set folder-id ${{ secrets.YC_FOLDER_ID }}

    - name: Build and push Docker image
      run: |
        docker build -t cr.yandex/${{ secrets.YC_REGISTRY_ID }}/trai-backend:${{ github.sha }} ./backend
        docker push cr.yandex/${{ secrets.YC_REGISTRY_ID }}/trai-backend:${{ github.sha }}

    - name: Deploy new container revision
      run: |
        yc serverless container revision deploy \
          --container-name trai-backend \
          --image cr.yandex/${{ secrets.YC_REGISTRY_ID }}/trai-backend:${{ github.sha }} \
          --cores 1 --memory 1GB \
          --environment XAI_API_KEY=${{ secrets.XAI_API_KEY }} \
          --environment JWT_SECRET=${{ secrets.JWT_SECRET }} \
          --environment MONGODB_URI=${{ secrets.MONGODB_URI }}
```

Required GitHub repository secrets:
```
YC_SA_JSON_CREDENTIALS   ← Yandex service account JSON key
YC_CLOUD_ID              ← Yandex Cloud ID
YC_FOLDER_ID             ← Yandex folder ID
YC_REGISTRY_ID           ← Yandex Container Registry ID
XAI_API_KEY              ← xAI Grok-2 API key
JWT_SECRET               ← JWT signing secret
MONGODB_URI              ← Production MongoDB connection string
```

---

## 11. Cloud Deployment — Yandex Cloud

### Step 1 — Install and configure Yandex CLI

```bash
curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash
yc init
```

### Step 2 — Authenticate with Container Registry

```bash
yc container registry configure-docker
```

### Step 3 — Create Container Registry

```bash
yc container registry create --name trai-registry
# Note the registry ID: crp_XXXXXXXX
```

### Step 4 — Build and push the Docker image

```bash
# Build JAR
cd backend && gradlew.bat clean bootJar -x test && cd ..

# Build Docker image (multi-stage — defined in backend/Dockerfile)
docker build -t trai-backend ./backend

# Tag for Yandex Container Registry
docker tag trai-backend cr.yandex/crp_XXXXXXXX/trai-backend:latest

# Push
docker push cr.yandex/crp_XXXXXXXX/trai-backend:latest
```

### Step 5 — Create Managed MongoDB

```bash
yc managed-mongodb cluster create \
  --name trai-mongo \
  --network-name default \
  --host zone-id=ru-central1-a,type=MONGOD \
  --mongodb-version 6.0 \
  --user name=trai,password=STRONG_PASSWORD \
  --database name=trai
```

### Step 6 — Create Managed Redis (optional, use in-memory otherwise)

```bash
yc managed-redis cluster create \
  --name trai-redis \
  --network-name default \
  --host zone-id=ru-central1-a \
  --redis-version 7
```

### Step 7 — Store secrets in Yandex Lockbox

```bash
yc lockbox secret create \
  --name trai-production-secrets \
  --payload '[
    {"key": "XAI_API_KEY",    "text_value": "your-xai-key"},
    {"key": "JWT_SECRET",     "text_value": "your-jwt-secret"},
    {"key": "MONGODB_URI",    "text_value": "mongodb://trai:PASSWORD@host:27017/trai"},
    {"key": "REDIS_HOST",     "text_value": "your-redis-host"},
    {"key": "WEBHOOK_SIGNING_SECRET", "text_value": "your-webhook-secret"}
  ]'
```

### Step 8 — Deploy Serverless Container

```bash
yc serverless container create --name trai-backend

yc serverless container revision deploy \
  --container-name trai-backend \
  --image cr.yandex/crp_XXXXXXXX/trai-backend:latest \
  --cores 1 \
  --memory 1GB \
  --concurrency 10 \
  --execution-timeout 30s \
  --environment SPRING_PROFILES_ACTIVE=prod \
  --environment XAI_API_KEY=$(yc lockbox payload get --name trai-production-secrets --key XAI_API_KEY) \
  --environment JWT_SECRET=$(yc lockbox payload get --name trai-production-secrets --key JWT_SECRET) \
  --environment MONGODB_URI=$(yc lockbox payload get --name trai-production-secrets --key MONGODB_URI)
```

### Step 9 — Deploy API Gateway

```bash
yc serverless api-gateway create \
  --name trai-gateway \
  --spec yandex-api-gateway.yaml
```

The gateway spec (`yandex-api-gateway.yaml`) proxies all `/api/v1/**` traffic to the backend Serverless Container.

### Step 10 — Deploy frontend to Object Storage

```bash
# Create bucket
yc storage bucket create --name trai-frontend-assets

# Upload static files
yc storage s3 cp frontend/ s3://trai-frontend-assets/ --recursive

# Enable static website hosting on bucket
yc storage bucket update trai-frontend-assets --website-settings '{"index": "index.html"}'
```

---

## 12. API Reference

Base URL (local): `http://localhost:8080/api/v1`

All authenticated endpoints require: `Authorization: Bearer <jwt-token>`

### Authentication

#### Register
```
POST /auth/register
Content-Type: application/json

{ "username": "alice", "email": "alice@example.com", "password": "StrongPass123!" }

Response 200: { "message": "User registered successfully" }
```

#### Login
```
POST /auth/login
Content-Type: application/json

{ "username": "alice", "password": "StrongPass123!" }

Response 200: { "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

---

### Core Verification

#### Health Check
```
GET /trust/status

Response 200:
{
  "application": "TrAI Trust Engine Platform",
  "status": "OPERATIONAL",
  "version": "0.0.1-SNAPSHOT"
}
```

#### Live Speech Fact-Check
```
POST /live/verify-statement
Authorization: Bearer <token>
Content-Type: application/json

{
  "speaker": "Donald Trump",
  "mediaSource": "Live Presidential Debate",
  "statement": "We put 25 percent tariffs on foreign steel and inflation dropped to zero."
}

Response 200:
{
  "speaker": "Donald Trump",
  "verdict": "MISLEADING",
  "trustScore": 38,
  "factCheckDetails": "...",
  "keyClaims": [
    { "claim": "25% steel tariffs", "status": "VERIFIED", "correction": null },
    { "claim": "inflation dropped to zero", "status": "DEBUNKED", "correction": "CPI was 2.4% in 2018" }
  ]
}
```

#### AI Hallucination Audit
```
POST /trust/verify-ai-output
Authorization: Bearer <token>
Content-Type: application/json

{
  "prompt": "Who signed the Kyoto Protocol on behalf of the US?",
  "aiResponse": "Signed by Bill Clinton and ratified unanimously by the Senate.",
  "modelName": "Internal Enterprise LLM"
}

Response 200:
{
  "modelAudited": "Internal Enterprise LLM",
  "isGlitchDetected": true,
  "glitchSeverity": "HIGH",
  "reliabilityScore": 24,
  "safeToPublish": false,
  "detectedHallucinations": ["Senate never ratified — only signed by VP Gore"]
}
```

#### News Propaganda Stripping
```
POST /trust/verify-news
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceName": "State Media",
  "text": "Hostile forces suffered catastrophic annihilation by our heroic defense units...",
  "sourceUrl": "https://example.com/article"
}

Response 200:
{
  "sourceName": "State Media",
  "normalizedText": "Military units engaged in combat operations in [region]...",
  "propagandaScore": 84,
  "flaggedPhrases": ["heroic defense", "catastrophic annihilation"],
  "neutralSummary": "..."
}
```

#### Source Credibility Index
```
GET /trust/sources
Authorization: Bearer <token>

Response 200:
[
  { "credibilityRank": 1, "sourceName": "Reuters", "trustScore": 94.5, "tier": "TIER_1_INSTITUTIONAL" },
  { "credibilityRank": 2, "sourceName": "BBC", "trustScore": 91.0, "tier": "TIER_1_INSTITUTIONAL" },
  ...
]
```

---

### Analytics

#### Market Impact Prediction
```
POST /analytics/market-impact
Authorization: Bearer <token>
Content-Type: application/json

{ "eventSummary": "Federal Reserve raised rates by 0.5% citing persistent inflation." }

Response 200:
{
  "gold": { "direction": "UP", "confidence": 78, "reasoning": "..." },
  "btc": { "direction": "DOWN", "confidence": 61, "reasoning": "..." },
  "oil": { "direction": "NEUTRAL", "confidence": 55, "reasoning": "..." },
  "usd": { "direction": "UP", "confidence": 82, "reasoning": "..." }
}
```

---

### B2B Webhook Gateway

#### Register Partner
```
POST /webhook/register
Content-Type: application/json

{
  "companyName": "FinTech Corp",
  "email": "tech@fintechcorp.com",
  "webhookCallbackUrl": "https://fintechcorp.com/trai-results"
}

Response 200:
{
  "partnerId": "uuid",
  "apiKey": "trai_live_xxxxx",
  "signingSecret": "whsec_xxxxx"
}
```

#### Ingest Verification Request
```
POST /webhook/ingest
X-TrAI-Partner-ID: your-partner-id
X-TrAI-Signature: hmac-sha256-of-body
Content-Type: application/json

{
  "requestType": "AI_AUDIT",
  "payload": {
    "prompt": "user question",
    "aiResponse": "model response",
    "modelName": "GPT-4"
  },
  "callbackUrl": "https://yourapp.com/callback"
}
```

Signature calculation:
```
HMAC-SHA256(signingSecret, requestBody)
```

---

### Supporting Endpoints

```
GET  /search/news?q=ukraine+gold             ← Full-text archive search
POST /translate                              ← Translate to EN/RU/HY
GET  /alerts                                 ← Platform incident alerts
GET  /audit/logs                             ← Compliance audit trail (ADMIN only)
POST /trust/kill-switch                      ← Toggle SentinelMind (ADMIN only)
GET  /webhook/status/{requestId}             ← B2B request status
```

---

## 13. Database Schema

Managed via Liquibase. Migration files live in `backend/src/main/resources/db/changelog/`.

### trai_users
```sql
id              VARCHAR(36) PRIMARY KEY
username        VARCHAR(50) UNIQUE NOT NULL
password_hash   VARCHAR(100) NOT NULL        -- BCrypt rounds=12
email           VARCHAR(100) UNIQUE NOT NULL
role            VARCHAR(20) NOT NULL         -- USER | BUSINESS | ADMIN
created_at      TIMESTAMP NOT NULL
```

### audit_trail_events
```sql
id              VARCHAR(36) PRIMARY KEY
actor           VARCHAR(100) NOT NULL
action_type     VARCHAR(50) NOT NULL
input_snippet   TEXT
verdict         VARCHAR(30)
trust_score     INTEGER
flags           TEXT
blocked         BOOLEAN
latency_ms      BIGINT
recorded_at     TIMESTAMP NOT NULL
```

### b2b_partners
```sql
id              VARCHAR(36) PRIMARY KEY
company_name    VARCHAR(100) NOT NULL
contact_email   VARCHAR(100) UNIQUE NOT NULL
signing_secret  VARCHAR(100) NOT NULL
callback_url    VARCHAR(500)
active          BOOLEAN DEFAULT TRUE
created_at      TIMESTAMP NOT NULL
```

---

## 14. Security Architecture

| Layer | Implementation | Details |
|---|---|---|
| **Rate Limiting** | Bucket4j token bucket | 120 requests/minute per IP, applied to all `/api/**` routes |
| **Authentication** | Stateless JWT (HMAC-SHA256) | Signed with `JWT_SECRET`, configurable expiration |
| **Authorization** | Spring Security RBAC | Roles: `USER`, `BUSINESS`, `ADMIN` |
| **Password hashing** | BCrypt, cost factor 12 | Applied at registration |
| **Input sanitization** | `InputSanitizerService` | HTML stripping (Jsoup), SQL injection (4 regex patterns), prompt injection (9 regex patterns), PII masking (email, card) |
| **Output guardrails** | `OutputGuardrailsService` | Blocks toxic content, redacts PII, blocks risk score ≥ 85 |
| **B2B webhook auth** | HMAC-SHA256 signatures | Every inbound webhook must include `X-TrAI-Signature` header |
| **Kill switch** | SentinelMind | Admin-toggleable — halts all AI pipelines instantly |
| **Secrets management** | Environment variables + Yandex Lockbox | API keys never committed to git |
| **CORS** | Spring Security | Configured for all `/api/**` routes |

---

## 15. Internationalization

The API supports three languages via `Accept-Language` header:

| Header | Language |
|---|---|
| `Accept-Language: en` | English (default) |
| `Accept-Language: ru` | Russian |
| `Accept-Language: hy` | Armenian |

Message bundles are in `backend/src/main/resources/`:
- `messages.properties` — English
- `messages_ru.properties` — Russian
- `messages_hy.properties` — Armenian

---

## 16. Troubleshooting

### Docker ports already in use

```bash
# Check what's using port 27017
netstat -ano | findstr :27017     # Windows
lsof -i :27017                    # macOS/Linux

docker-compose down && docker-compose up -d
```

### Backend fails: "Connection refused: localhost:27017"

MongoDB is not running. Start infrastructure first:
```bash
docker-compose up -d mongodb redis
# Wait 5-10 seconds, then:
cd backend && ./gradlew bootRun
```

### AI calls fail with "401 Unauthorized"

API key not set or incorrect:
```bash
echo $XAI_API_KEY         # macOS/Linux
echo %XAI_API_KEY%        # Windows CMD
echo $env:XAI_API_KEY     # Windows PowerShell
```

### Frontend shows "Standalone Demo Mode" even when backend is running

CORS issue. Verify backend is running on port 8080 and `SecurityConfig` permits the origin. Open browser dev tools → Network tab → look for `OPTIONS` preflight failures.

### gradlew: Permission denied (macOS/Linux)

```bash
chmod +x backend/gradlew
```

### Java version mismatch

```bash
java -version   # Must show openjdk version "21.x.x"

# macOS (Homebrew):
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Windows: Set JAVA_HOME environment variable to your JDK 21 installation path
```

### Build fails during tests (MongoDB connection)

Tests use embedded MongoDB (`flapdoodle`). No Docker needed for tests. If tests still fail:
```bash
./gradlew clean check --rerun-tasks
```

---

## 17. Contributing

### Git workflow

```bash
# 1. Clone the repository
git clone git@github.com:gariktepanosian/trAi.git
cd trAi

# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Make changes, write tests

# 4. Run tests before committing
cd backend && ./gradlew check

# 5. Commit (conventional commit format)
git add .
git commit -m "feat: add real-time WebSocket streaming for live fact-check"

# 6. Push and open a pull request
git push -u origin feature/your-feature-name
```

### Commit message convention

```
feat:     new feature
fix:      bug fix
refactor: code refactoring without feature change
test:     adding or fixing tests
docs:     documentation only
chore:    build / CI changes
```

### Branch protection

The `main` branch requires:
- All CI jobs passing (backend tests + frontend validation + Docker Compose validation)
- At least one peer review approval

---

## Quick Reference Cheat Sheet

```bash
# Start infrastructure
docker-compose up -d mongodb redis

# Build backend
cd backend && gradlew.bat build -x test        # Windows
cd backend && ./gradlew build -x test           # macOS/Linux

# Run backend
cd backend && gradlew.bat bootRun               # Windows
cd backend && ./gradlew bootRun                 # macOS/Linux

# Run tests
cd backend && gradlew.bat check                 # Windows
cd backend && ./gradlew check                   # macOS/Linux

# Open frontend
start frontend/index.html                       # Windows
open frontend/index.html                        # macOS

# Check backend health
curl http://localhost:8080/api/v1/trust/status

# View logs
docker-compose logs -f trai-backend

# Stop everything
docker-compose down

# Full reset (wipes all data)
docker-compose down -v && docker-compose up -d
```
