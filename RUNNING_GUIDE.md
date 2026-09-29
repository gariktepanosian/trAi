# TrAI Platform — Complete Running Guide
> **How to run, test, and work with the TrAI platform end-to-end**  
> Covers: local development, demo mode, backend + frontend, Docker, and first deployment steps.

---

## TABLE OF CONTENTS

1. [What Is This App](#1-what-is-this-app)
2. [Prerequisites — What You Need Installed](#2-prerequisites)
3. [Project Structure](#3-project-structure)
4. [OPTION A — Run the Demo Immediately (No Backend Needed)](#4-option-a-demo-mode)
5. [OPTION B — Run the Full Stack Locally](#5-option-b-full-stack-local)
6. [Running With Docker Compose](#6-docker-compose)
7. [API Endpoints Reference](#7-api-endpoints)
8. [How the Webhook Catcher Works (B2B)](#8-webhook-catcher-b2b)
9. [How the B2C Consumer Side Works](#9-b2c-consumer-side)
10. [Environment Variables Reference](#10-environment-variables)
11. [Internationalization (i18n)](#11-internationalization)
12. [Troubleshooting](#12-troubleshooting)
13. [Yandex Cloud Deployment Steps](#13-yandex-cloud-deployment)
14. [Git Setup and Push to GitHub](#14-git-push)

---

## 1. WHAT IS THIS APP

TrAI is a **Trust-as-a-Service (TaaS)** platform that sits between raw information sources and downstream consumers — journalists, businesses, and end users.

It does three core things:

1. **Live Speech Fact-Checking** — Takes a transcribed spoken statement and verifies it against ground truth
2. **AI Hallucination Auditing** — Takes an AI model's response and checks if it contains fabricated facts
3. **News Propaganda Stripping** — Takes a raw scraped article and normalizes it to neutral factual language

The system has two sides:
- **B2C (Consumer side):** A web/mobile app for individuals — journalists, researchers, news consumers
- **B2B (Enterprise side):** A webhook API for businesses — broadcast networks, fintech, legal platforms

---

## 2. PREREQUISITES

Install the following before proceeding:

### Required

| Tool | Version | Purpose | Download |
|------|---------|---------|----------|
| **Java JDK** | 21 (LTS) | Run Spring Boot backend | https://adoptium.net |
| **Docker Desktop** | Latest | Run MongoDB + Redis | https://docker.com/products/docker-desktop |
| **Git** | Latest | Version control | https://git-scm.com |

### Optional (for full features)

| Tool | Version | Purpose |
|------|---------|---------|
| **Gradle** | 8.x (included via wrapper) | Build tool — use `./gradlew` instead |
| **IntelliJ IDEA** | 2024.x+ | Java IDE (recommended) |
| **Node.js** | 20 LTS | Only needed if you convert frontend to React later |
| **Flutter SDK** | 3.x | Only needed for mobile app (not yet implemented) |

### API Keys Required (get these before running)

| Key | Where to Get | Used For |
|-----|-------------|---------|
| `XAI_API_KEY` | https://console.x.ai | Grok-2 — main AI engine |
| `GEMINI_PROJECT_ID` | Google Cloud Console | Vertex AI Gemini (alternative model) |
| `GEMINI_LOCATION` | Google Cloud Console | Region (e.g., `us-central1`) |

> **For demo mode:** You do NOT need API keys. The frontend runs entirely in simulation mode without any keys.

---

## 3. PROJECT STRUCTURE

```
TrAI/              ← root project directory
│
├── backend/                  ← Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/trai/engine/    ← Java source (rename to com/trai/engine)
│   │   │   │   ├── ai/                         ← LangChain4j AI engine interfaces
│   │   │   │   │   ├── AntiPropagandaEngine.java
│   │   │   │   │   ├── LiveFactCheckEngine.java
│   │   │   │   │   └── AiGlitchVerifierEngine.java
│   │   │   │   ├── config/                     ← Spring configuration (Grok-2 + Gemini setup)
│   │   │   │   │   └── AiConfig.java
│   │   │   │   ├── controller/                 ← REST API endpoints
│   │   │   │   │   └── TrustEngineController.java
│   │   │   │   ├── domain/                     ← MongoDB document models
│   │   │   │   │   ├── NormalizedNews.java
│   │   │   │   │   ├── SourceTrustScore.java
│   │   │   │   │   ├── InsiderInfo.java
│   │   │   │   │   └── VerifiedClaim.java
│   │   │   │   ├── dto/                        ← Request/Response objects
│   │   │   │   │   ├── LiveStatementRequest.java
│   │   │   │   │   ├── AiGlitchCheckRequest.java
│   │   │   │   │   └── NewsVerificationRequest.java
│   │   │   │   ├── repository/                 ← MongoDB data access
│   │   │   │   │   ├── NormalizedNewsRepository.java
│   │   │   │   │   └── SourceTrustScoreRepository.java
│   │   │   │   ├── scheduler/                  ← Scheduled jobs (scraping every 10 min)
│   │   │   │   │   └── NewsScrapingScheduler.java
│   │   │   │   └── service/                    ← Business logic
│   │   │   │       └── TrustVerificationService.java
│   │   │   └── resources/
│   │   │       └── application.yml             ← Configuration (DB, API keys)
│   │   └── test/
│   ├── build.gradle
│   └── gradlew / gradlew.bat
│
├── frontend/                 ← Web frontend (plain HTML/CSS/JS)
│   ├── index.html            ← Main app page (4-tab UI)
│   ├── style.css             ← Dark theme styles
│   └── app.js                ← Frontend logic + API calls + fallback simulations
│
├── docker-compose.yml        ← Starts MongoDB + Redis locally
├── GAPS_AND_STATUS.md        ← This audit document
├── RUNNING_GUIDE.md          ← This file
├── BUSINESS_PRESENTATION.md  ← Pitch deck content
└── README.md
```

---

## 4. OPTION A — DEMO MODE (No Backend Needed)

This is the fastest way to see the app working. **No API keys, no Java, no Docker required.**

### Steps:

**Step 1:** Open the frontend folder:
```
TrAI/frontend/
```

**Step 2:** Open `index.html` directly in your browser.

On Windows:
- Double-click `index.html`
- OR right-click → "Open with" → Chrome/Firefox/Edge

On macOS:
```bash
open "frontend/index.html"
```

**Step 3:** The app loads. The header will show:
```
Backend: Standalone Demo Mode (Active)
```
This is normal — it means the frontend is running without a backend.

**Step 4:** Try all 4 features:

| Tab | What to Do | What You See |
|-----|-----------|-------------|
| **Live Video Fact-Checker** | Click "Donald Trump (Economic Claim)" → "Audit Live Statement" | Returns MISLEADING verdict with claim breakdown |
| **AI Glitch Auditor** | Click "Fabricated Treaty Citation" → "Audit for Glitches" | Returns CRITICAL HALLUCINATION with ground truth correction |
| **News Propaganda Stripper** | Click "Strip Propaganda & Extract Facts" | Returns neutral factual extraction |
| **Source Trust Index** | Loads automatically | Shows 6 media sources with trust scores |

> **This is your 5-minute demo** — use this for investor or client presentations.

---

## 5. OPTION B — FULL STACK LOCAL

This runs the real Spring Boot backend with actual AI API calls.

### Step 1: Start Infrastructure (MongoDB + Redis)

Open a terminal in the project root:

```bash
docker-compose up -d
```

Verify they are running:
```bash
docker ps
```

You should see:
```
trai-mongo    ← MongoDB on port 27017
trai-redis    ← Redis on port 6379
```

### Step 2: Set Environment Variables

**On Windows (PowerShell):**
```powershell
$env:XAI_API_KEY = "your-actual-grok-api-key-here"
$env:GEMINI_PROJECT_ID = "your-google-cloud-project-id"
$env:GEMINI_LOCATION = "us-central1"
```

**On Windows (Command Prompt):**
```cmd
set XAI_API_KEY=your-actual-grok-api-key-here
set GEMINI_PROJECT_ID=your-google-cloud-project-id
set GEMINI_LOCATION=us-central1
```

**On macOS/Linux:**
```bash
export XAI_API_KEY=your-actual-grok-api-key-here
export GEMINI_PROJECT_ID=your-google-cloud-project-id
export GEMINI_LOCATION=us-central1
```

> **Without real API keys:** The backend will start but AI calls will fail with authentication errors. Use demo mode (Option A) instead.

### Step 3: Build and Run the Backend

Navigate to the backend directory:

```bash
cd backend
```

**On macOS/Linux:**
```bash
./gradlew bootRun
```

**On Windows:**
```cmd
gradlew.bat bootRun
```

Wait for this output:
```
Started TrAIEngine in X.XXX seconds
Tomcat started on port 8080
```

### Step 4: Verify Backend is Running

Open a browser or use curl:
```bash
curl http://localhost:8080/api/v1/trust/status
```

Expected response:
```json
{
  "status": "OPERATIONAL",
  "version": "1.0.0"
}
```

### Step 5: Open the Frontend

Open `frontend/index.html` in your browser.

The header status badge should now show:
```
Backend: OPERATIONAL (v1.0.0)
```
(green color)

### Step 6: Test a Real AI-Powered Request

In Tab 1 (Live Fact-Checker):
1. Select the Trump preset
2. Click "Audit Live Statement"
3. Wait 2–5 seconds (real Grok-2 API call)
4. Response comes from actual AI model, not simulation

---

## 6. RUNNING WITH DOCKER COMPOSE

Currently `docker-compose.yml` only starts the infrastructure (MongoDB + Redis). To add the backend app container, create a `Dockerfile` in `backend/`:

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Copy pre-built JAR (build first with: ./gradlew build -x test)
COPY build/libs/*.jar app.jar

# Support for Cyrillic and Armenian character encoding
RUN apk add --no-cache ttf-dejavu

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar", \
  "--spring.profiles.active=prod"]
```

Then extend `docker-compose.yml`:

```yaml
# Add this service to docker-compose.yml:
  trai-backend:
    build: ./backend
    container_name: trai-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/trai
      - SPRING_DATA_REDIS_HOST=redis
      - XAI_API_KEY=${XAI_API_KEY}
      - GEMINI_PROJECT_ID=${GEMINI_PROJECT_ID}
      - GEMINI_LOCATION=${GEMINI_LOCATION}
    depends_on:
      - mongodb
      - redis
    restart: unless-stopped
```

Build and run everything:
```bash
# Build JAR first
cd backend && gradlew.bat build -x test && cd ..

# Start all containers
docker-compose up -d --build

# Watch logs
docker-compose logs -f trai-backend
```

---

## 7. API ENDPOINTS REFERENCE

Base URL: `http://localhost:8080/api/v1`

### Health Check
```
GET /trust/status
Response: { "status": "OPERATIONAL", "version": "1.0.0" }
```

### Live Speech Fact-Checking
```
POST /live/verify-statement
Content-Type: application/json

{
  "speaker": "Donald Trump",
  "statement": "We put 25 percent tariffs on foreign steel...",
  "mediaSource": "Live Rally Stream"
}

Response:
{
  "speaker": "Donald Trump",
  "verdict": "MISLEADING",
  "trustScore": 38,
  "factCheckDetails": "...",
  "keyClaims": [
    { "claim": "...", "status": "VERIFIED", "correction": "..." }
  ]
}
```

### AI Hallucination Audit
```
POST /trust/verify-ai-output
Content-Type: application/json

{
  "prompt": "Who signed the Kyoto Protocol?",
  "aiResponse": "Bill Clinton signed it...",
  "modelName": "ChatGPT-4"
}

Response:
{
  "modelAudited": "ChatGPT-4",
  "isGlitchDetected": true,
  "glitchSeverity": "HIGH",
  "reliabilityScore": 24,
  "safeToPublish": false,
  "detectedHallucinations": [...]
}
```

### News Verification
```
POST /trust/verify-news
Content-Type: application/json

{
  "text": "In a cowardly display...",
  "sourceUrl": "https://example.com/article",
  "sourceName": "State Media"
}
```

### Source Trust Scores
```
GET /trust/sources
Response: [
  { "credibilityRank": 1, "sourceName": "Reuters", "trustScore": 94.5, ... }
]
```

---

## 8. WEBHOOK CATCHER — B2B SIDE

> **Status: Architecture defined, implementation pending** (see GAPS_AND_STATUS.md)

### What it does

The webhook catcher allows **external businesses** to POST a JSON payload to TrAI and receive a structured verification response. The gateway automatically classifies the request type and routes it to the correct internal service.

### How it will work (specification)

```
External Partner System
        │
        ▼
POST /api/v1/webhook/ingest
Headers:
  X-TrAI-Partner-ID: your-partner-id
  X-TrAI-Signature: hmac-sha256-of-body
  Content-Type: application/json

Body (any of these formats):

{
  "requestType": "NEWS_VERIFY",      ← Gateway classifies based on this
  "payload": {
    "text": "raw article text...",
    "sourceName": "CNN"
  },
  "callbackUrl": "https://yourapp.com/trai-callback"   ← optional async response
}

OR:

{
  "requestType": "AI_AUDIT",
  "payload": {
    "prompt": "user question",
    "aiResponse": "model response"
  }
}

OR:

{
  "requestType": "LIVE_FACT_CHECK",
  "payload": {
    "speaker": "CEO Name",
    "statement": "Our Q3 profits were up 300%"
  }
}
```

### Synchronous Response (small payloads):
```json
{
  "requestId": "uuid-here",
  "requestType": "AI_AUDIT",
  "status": "COMPLETED",
  "result": { ... },
  "processingTimeMs": 1245,
  "trustScore": 82
}
```

### How to Register as a B2B Partner:
```
POST /api/v1/webhook/register
{
  "companyName": "Your Company",
  "email": "tech@yourcompany.com",
  "webhookCallbackUrl": "https://yourapp.com/trai-results"
}

Response:
{
  "partnerId": "uuid",
  "apiKey": "trai_live_xxxxx",
  "signingSecret": "whsec_xxxxx"
}
```

### Security (HMAC Signature Verification):

Every inbound webhook request must include an `X-TrAI-Signature` header:
```
HMAC-SHA256(signingSecret, requestBody)
```

The server verifies this before processing. Any request without a valid signature returns `403 Forbidden`.

---

## 9. B2C CONSUMER SIDE

### Current State (Web)

The web frontend at `frontend/index.html` IS the B2C consumer side. Consumers:

1. Open the web app
2. Paste a statement, AI response, or news article
3. Get an immediate verification result
4. Trust scores for media sources are visible in Tab 4

### Future B2C Features (not yet built)

1. **User Accounts** — Register, log in, save searches
2. **Topic Pipelines** — "Alert me whenever there is verified news about Iran + gold price"
3. **Mobile App (Flutter)** — iOS and Android versions with push notifications
4. **Telegram Bot** — Send a message to `@trAIbot`, get instant fact-check

---

## 10. ENVIRONMENT VARIABLES REFERENCE

### `application.yml` — What Each Variable Means

```yaml
spring:
  application:
    name: TrAIEngine
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/trai}
      # Production: set MONGODB_URI to your Yandex Managed MongoDB URI
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

xai:
  api-key: ${XAI_API_KEY:your-xai-api-key}
  # Get from: https://console.x.ai
  # Used for: Grok-2 — main fact-checking and propaganda detection AI

langchain4j:
  vertex-ai:
    gemini:
      project: ${GEMINI_PROJECT_ID:your-project-id}
      # Get from: Google Cloud Console → Project ID
      location: ${GEMINI_LOCATION:us-central1}
      # Usually us-central1 or europe-west1
      model-name: gemini-1.5-pro
      # Used for: secondary AI model (consensus validation)
```

### Full `.env.example` (create this file, never commit actual values)

```env
# Copy this to .env and fill in real values
# NEVER commit .env to git

# MongoDB (local = auto, production = Yandex Managed MongoDB URI)
MONGODB_URI=mongodb://localhost:27017/trai

# Redis (local = auto, production = Yandex ElastiCache or Managed Redis)
REDIS_HOST=localhost
REDIS_PORT=6379

# xAI Grok-2 (REQUIRED for real AI responses)
XAI_API_KEY=xai_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

# Google Cloud Vertex AI Gemini (optional second model)
GEMINI_PROJECT_ID=your-google-cloud-project-id
GEMINI_LOCATION=us-central1

# JWT Secret (needed when auth is implemented)
JWT_SECRET=change-this-to-a-very-long-random-string-at-least-256-bits

# Yandex Object Storage (needed for file/media uploads)
YANDEX_STORAGE_KEY=your-yandex-storage-access-key
YANDEX_STORAGE_SECRET=your-yandex-storage-secret-key
```

### Setting Variables in Yandex Cloud Lockbox

```bash
# Install Yandex CLI first: https://cloud.yandex.com/docs/cli/quickstart

# Create a Lockbox secret
yc lockbox secret create \
  --name trai-secrets \
  --payload '[
    {"key": "XAI_API_KEY", "text_value": "your-key"},
    {"key": "GEMINI_PROJECT_ID", "text_value": "your-project"},
    {"key": "JWT_SECRET", "text_value": "your-secret"}
  ]'

# When deploying to Yandex Container Registry / MKS,
# mount the Lockbox secret as environment variables in your
# container spec or Kubernetes secret
```

---

## 11. INTERNATIONALIZATION (i18n)

> **Status: Not yet implemented** — here is exactly how to add it.

### Step 1: Create message files in `backend/src/main/resources/`

**`messages.properties`** (default — English):
```properties
error.statement.empty=Statement cannot be empty.
error.ai.response.empty=AI response cannot be empty.
verdict.verified=VERIFIED TRUE
verdict.false=FALSE
verdict.misleading=MISLEADING
source.tier.1=TIER 1 - INSTITUTIONAL
api.status.operational=OPERATIONAL
```

**`messages_ru.properties`** (Russian):
```properties
error.statement.empty=Высказывание не может быть пустым.
error.ai.response.empty=Ответ AI не может быть пустым.
verdict.verified=ПОДТВЕРЖДЕНО
verdict.false=ЛОЖЬ
verdict.misleading=ВВОДЯЩЕЕ В ЗАБЛУЖДЕНИЕ
source.tier.1=УРОВЕНЬ 1 - ИНСТИТУЦИОНАЛЬНЫЙ
api.status.operational=РАБОТАЕТ
```

**`messages_am.properties`** (Armenian):
```properties
error.statement.empty=Հայտարարությունը չի կարող դատարկ լինել:
error.ai.response.empty=AI-ի պատասխանը չի կարող դատարկ լինել:
verdict.verified=ՀԱՍՏԱՏՎԱԾ
verdict.false=ԿԵՂԾ
verdict.misleading=ՄՈԼՈՐԵՑՆՈՂ
source.tier.1=ՄԱԿԱՐԴԱԿ 1 - ԻՆՍՏԻՏՈՒՑԻՈՆԱԼ
api.status.operational=ԱՇԽԱՏՈՒՄ Է
```

### Step 2: Add `LocaleResolver` and `MessageSource` beans to `AiConfig.java` or a new `I18nConfig.java`:

```java
@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
```

### Step 3: Use in Controller:

```java
@Autowired
private MessageSource messageSource;

// In any method:
String errorMessage = messageSource.getMessage(
    "error.statement.empty",
    null,
    LocaleContextHolder.getLocale()
);
```

### Step 4: Client sends language header:
```
Accept-Language: ru
Accept-Language: hy  ← Armenian
Accept-Language: en  ← English (default)
```

---

## 12. TROUBLESHOOTING

### Problem: `docker-compose up` fails — port already in use

```bash
# Check what's using port 27017 (MongoDB)
netstat -ano | findstr :27017   # Windows
lsof -i :27017                  # macOS/Linux

# Stop Docker containers and try again
docker-compose down
docker-compose up -d
```

### Problem: Backend fails to start — `Connection refused: localhost:27017`

MongoDB is not running. Start it first:
```bash
docker-compose up -d mongodb
# Wait 5 seconds, then start backend
```

### Problem: Backend compiles but AI calls fail — `401 Unauthorized`

Your API key is not set or is wrong.
```bash
# Verify environment variable is set
echo $XAI_API_KEY     # macOS/Linux
echo %XAI_API_KEY%    # Windows CMD
echo $env:XAI_API_KEY # Windows PowerShell
```

### Problem: Frontend says "Backend: Standalone Demo Mode" even when backend is running

CORS is not configured. The backend rejects requests from `file://` or different origins.

Temporary fix — add to `AiConfig.java` or new `WebConfig.java`:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

Then rebuild and restart.

### Problem: `gradlew: Permission denied` on macOS/Linux

```bash
chmod +x backend/gradlew
```

### Problem: Build fails — `Kotlin/Java version incompatibility`

Ensure Java 21 is the active JDK:
```bash
java -version
# Should show: openjdk version "21.x.x"
```

If wrong version:
```bash
# macOS with Homebrew:
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Windows: Set JAVA_HOME in System Environment Variables
# pointing to your JDK 21 installation directory
```

---

## 13. YANDEX CLOUD DEPLOYMENT STEPS

### Prerequisites
```bash
# Install Yandex CLI
curl -sSL https://storage.yandexcloud.net/yandexcloud-yc/install.sh | bash
yc init
```

### Step 1: Create Container Registry
```bash
yc container registry create --name trai-registry
# Note the registry ID: crp_XXXXXXXX
```

### Step 2: Build and Push Docker Image
```bash
# Build backend JAR
cd backend && gradlew.bat build -x test && cd ..

# Build Docker image (create Dockerfile in backend/ first — see Section 6)
docker build -t trai-backend ./backend

# Tag for Yandex registry
docker tag trai-backend cr.yandex/crp_XXXXXXXX/trai-backend:latest

# Push
docker push cr.yandex/crp_XXXXXXXX/trai-backend:latest
```

### Step 3: Create Managed MongoDB
```bash
yc managed-mongodb cluster create \
  --name trai-mongo \
  --network-name default \
  --host zone-id=ru-central1-a,type=MONGOD \
  --mongodb-version 6.0 \
  --user name=trai,password=STRONG_PASSWORD \
  --database name=trai
```

### Step 4: Create Serverless Container (simplest option)
```bash
yc serverless container create --name trai-backend
yc serverless container revision deploy \
  --container-name trai-backend \
  --image cr.yandex/crp_XXXXXXXX/trai-backend:latest \
  --cores 1 \
  --memory 512m \
  --environment XAI_API_KEY=your_key \
  --environment MONGODB_URI=mongodb://trai:PASSWORD@host:27017/trai
```

### Step 5: Set Up Yandex API Gateway
Create `gateway-spec.yaml`:
```yaml
openapi: 3.0.0
info:
  title: TrAI API Gateway
  version: 1.0.0
paths:
  /api/v1/trust/{path+}:
    x-yc-apigateway-any-method:
      x-yc-apigateway-integration:
        type: http
        url: https://your-container-url.containers.yandexcloud.net/api/v1/trust/{path}
        method: '*'
        headers:
          X-Forwarded-For: '{sourceIp}'
  /api/v1/live/{path+}:
    x-yc-apigateway-any-method:
      x-yc-apigateway-integration:
        type: http
        url: https://your-container-url.containers.yandexcloud.net/api/v1/live/{path}
        method: '*'
  /api/v1/webhook/{path+}:
    x-yc-apigateway-any-method:
      x-yc-apigateway-integration:
        type: http
        url: https://your-container-url.containers.yandexcloud.net/api/v1/webhook/{path}
        method: '*'
```

```bash
yc serverless api-gateway create \
  --name trai-gateway \
  --spec gateway-spec.yaml
```

---

## 14. GIT SETUP AND PUSH TO GITHUB

Run these commands from the project root directory (`TrAI/`):

```bash
# Step 1: Initialize git repository
git init

# Step 2: Create .gitignore if not present (already exists in project)
# Verify sensitive files are excluded:
# - .env files
# - build/ directories
# - .idea/ IDE files

# Step 3: Add all files
git add .

# Step 4: Create first commit
git commit -m "feat: initial TrAI platform — trust-as-a-service engine"

# Step 5: Rename branch to main
git branch -M main

# Step 6: Add remote origin (your GitHub repository)
git remote add origin git@github.com:gariktepanosian/trAi.git

# Step 7: Push to GitHub
git push -u origin main
```

### If you get SSH key errors:

Option A — Use HTTPS instead of SSH:
```bash
git remote set-url origin https://github.com/gariktepanosian/trAi.git
git push -u origin main
# Enter GitHub username and personal access token when prompted
```

Option B — Generate SSH key:
```bash
ssh-keygen -t ed25519 -C "your-email@example.com"
cat ~/.ssh/id_ed25519.pub
# Copy output and add to GitHub: Settings → SSH Keys → New SSH Key
```

### Verify push succeeded:
```bash
git remote -v
git log --oneline
git status
```

---

## QUICK REFERENCE CHEAT SHEET

```
START INFRASTRUCTURE:   docker-compose up -d
BUILD BACKEND:          cd backend && gradlew.bat build -x test
RUN BACKEND:            cd backend && gradlew.bat bootRun
OPEN FRONTEND:          Open frontend/index.html in browser
CHECK BACKEND HEALTH:   curl http://localhost:8080/api/v1/trust/status
STOP EVERYTHING:        docker-compose down
VIEW LOGS:              docker-compose logs -f
REBUILD FROM SCRATCH:   docker-compose down -v && docker-compose up -d
```
