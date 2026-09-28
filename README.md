# TrAI — Enterprise Truth Infrastructure & Real-Time Fact-Checking Platform
> **Trust-as-a-Service (TaaS) Engine for Media, Finance, and Enterprise AI Guardrails**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.29.1-blue.svg)](https://github.com/langchain4j/langchain4j)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%207.0-green.svg)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis%207.0-red.svg)](https://redis.io/)
[![xAI Grok & Gemini](https://img.shields.io/badge/AI%20Engines-xAI%20Grok--2%20%7C%20Gemini%201.5%20Pro-purple.svg)](https://x.ai)

---

## 1. Executive Summary & B2B Sales Presentation

### The Core Problem: The $78B Epistemic Crisis
Modern enterprises, media broadcast networks, financial institutions, and government bodies face an unprecedented challenge: **information pollution, deepfakes, hallucinated AI outputs, and hyper-partisan state-sponsored propaganda.**
* **For News Organizations:** Ingesting raw wire feeds and broadcasting live political speeches or debates without instant fact verification results in devastating loss of subscriber trust, regulatory fines, and billion-dollar defamation lawsuits.
* **For Financial Institutions:** Algorithmic trading and risk models can be compromised by fake press releases, synthetic market rumors, and unverified social media leaks.
* **For Enterprise AI Deployments:** Deploying LLMs (Customer Support, Legal, Finance) exposes companies to severe hallucination glitches where models invent non-existent laws, hallucinate numbers, or produce damaging statements.

### TrAI Solution: "Trust-as-a-Service" (TaaS)
**TrAI** is not just an application; it is an **institutional Truth Layer**. It operates as an autonomous, high-throughput verification gateway that inspects incoming data (news articles, live broadcast audio streams, or LLM-generated text) and evaluates it against empirical consensus, historical knowledge graphs, and cross-source corroboration.

```
       [ Live Video / Speech Streams ]   [ Raw News Articles ]   [ LLM-Generated Responses ]
                      │                            │                          │
                      ▼                            ▼                          ▼
      ┌─────────────────────────────────────────────────────────────────────────────┐
      │                         TrAI VERIFICATION GATEWAY                           │
      │  ┌─────────────────────────┐  ┌──────────────────────┐  ┌────────────────┐  │
      │  │ Live Speech Fact-Check  │  │ Anti-Propaganda      │  │ AI Glitch &    │  │
      │  │ Engine (Debates, Trump, │  │ Normalizer & Consensus│  │ Hallucination  │  │
      │  │ Breaking Broadcasts)    │  │ Extractor            │  │ Auditor        │  │
      │  └───────────┬─────────────┘  └───────────┬──────────┘  └────────┬───────┘  │
      │              │                            │                      │          │
      │              ▼                            ▼                      ▼          │
      │     [xAI Grok-2 Engine]        [Google Vertex AI Gemini]    [Redis Cache]   │
      └─────────────────────────────────────┬───────────────────────────────────────┘
                                            │
                                            ▼
               [ Instant Fact Verdict: TRUE | FALSE | MISLEADING ]
               [ Source Credibility Score (0 - 100%) ]
               [ Radically Neutral Consensus JSON Feed ]
```

---

## 2. B2B Commercialization: How to Pitch and Sell This Platform

When pitching TrAI to B2B clients, **you are not merely selling a software license—you are selling high-margin Enterprise Support, Continuous Truth Intelligence, and Risk Mitigation SLAs.**

### Primary Target Markets
1. **Broadcast & Digital News Networks (e.g., Bloomberg, Reuters, CNN, BBC, regional outlets):**
   * *Pitch:* "Embed TrAI's real-time live video transcription plugin into your broadcast control room. When political figures (e.g., Donald Trump, world leaders, candidates) speak live on air, your production team receives instant claim-by-claim fact-checking with verified historical data points within sub-second latency."
2. **FinTech, Hedge Funds, & Asset Managers:**
   * *Pitch:* "Protect your algorithmic strategies against synthetic market rumors and unverified financial leaks. TrAI flags unverified claims with 'RED INSIDER STATUS' before capital is committed."
3. **Enterprise LLM Deployments (Fortune 500 AI Adopters):**
   * *Pitch:* "Use TrAI as an external firewall. Before any internal AI response reaches your customers, TrAI audits the response to verify citations, eliminate hallucinations, and prevent corporate liability."

### Monetization Model: "Software + Enterprise Support & SLA"
Do not sell this as a one-time hand-off. The recurring revenue comes from:
* **Tier 1: Enterprise TaaS API Subscription:** Tiered volume pricing based on monthly verification calls ($5,000 to $50,000/month).
* **Tier 2: Dedicated Managed Support & Custom SLAs:**
  * 24/7 dedicated engineering support with guaranteed 99.99% uptime.
  * Custom media stream ingestion connectors (RTSP/RTMP video feeds, Bloomberg terminal feeds, SDI broadcast pipes).
  * Private air-gapped on-premise deployments for defense, intelligence, or regulated banking.
* **Tier 3: Domain Customization & Model Fine-Tuning:**
  * Training domain-specific fact-check databases (e.g., Pharmaceutical FDA filings, SEC filings, International Maritime Law).

---

## 3. Technical Architecture & Code-Level Status

### What is Implemented in Code Level
* [x] **Live Video / Speech Fact-Check Engine** (`LiveFactCheckEngine.java`, `LiveStatementRequest.java`): Real-time analysis of transcribed spoken claims from public figures/debates; extracts claims and provides an empirical verdict (`VERIFIED_TRUE`, `FALSE`, `MISLEADING`, `UNVERIFIED_CLAIM`) with factual anchors.
* [x] **AI Hallucination & Glitch Auditor** (`AiGlitchVerifierEngine.java`, `AiGlitchCheckRequest.java`): Independent audit layer detecting hallucinations, inverted facts, and fabricated citations in LLM responses.
* [x] **Radical Neutrality / Anti-Propaganda Engine** (`AntiPropagandaEngine.java`): Standardizes double standards, removes emotional appeals, and isolates verified facts.
* [x] **B2B REST API Controller** (`TrustEngineController.java`): Endpoints for live verification, AI glitch auditing, news normalization, and source rankings with full CORS support.
* [x] **Data Persistence & Domain Model** (`NormalizedNews`, `SourceTrustScore`, `InsiderInfo`, `VerifiedClaim`): MongoDB persistence for verified reports and Redis caching infrastructure.
* [x] **Automated Ingestion Pipeline** (`NewsScrapingScheduler.java`): Scheduled scraper using Jsoup for automated web consensus ingestion.
* [x] **Interactive High-Tech Web UI** (`frontend/index.html`, `frontend/style.css`, `frontend/app.js`): Terminal-style dashboard featuring live speech fact-checking simulator (with Donald Trump presets, debate quotes), AI hallucination audits, and live source trust index.

### Technology Stack
* **Backend Framework:** Spring Boot 4.0.3 (Java 21)
* **AI Orchestration:** LangChain4j (v0.29.1)
* **Underlying LLMs:** xAI Grok-2 (`grok-2-latest`) & Google Cloud Vertex AI Gemini (`gemini-1.5-pro`)
* **Databases:** MongoDB (Documents) & Redis (Real-time caching & fast trust score retrieval)
* **Web Scraping:** Jsoup 1.17.2
* **Frontend:** Modern Vanilla CSS (Glassmorphism, Dark Mode, Space Grotesk / JetBrains Mono typography) & Vanilla JS

---

## 4. Supported Platforms & Environments

TrAI can be deployed across the following operating systems and cloud environments:

| Platform | Deployment Mode | Supported Versions / Notes |
| :--- | :--- | :--- |
| **Windows** | Native / Local IDE | Windows 10, Windows 11 (PowerShell / CMD / IntelliJ / Eclipse / VS Code) |
| **Linux** | Native / Systemd / Container | Ubuntu 20.04+, Debian 11+, RHEL/CentOS 8+, Alpine Linux |
| **macOS** | Native / Homebrew | Apple Silicon (M1/M2/M3/M4) and Intel x86_64 |
| **Docker** | Containerized | Docker Engine 24+ & Docker Compose v2+ (Included `docker-compose.yml`) |
| **Kubernetes** | Orchestrated Pods | Helm / K8s manifests for enterprise horizontal scaling |
| **Cloud Providers** | Cloud Native | AWS (ECS/EKS), Google Cloud (GKE / Cloud Run), Azure (AKS) |

---

## 5. Step-by-Step Guide: How to Run the Application

### Prerequisites
1. **Java Development Kit (JDK):** Version 17 or 21 (Java 21 recommended). Verify with `java -version`.
2. **Docker & Docker Compose:** For running local MongoDB and Redis instances.
3. **API Keys (Optional for live LLM mode):**
   * xAI API Key (`XAI_API_KEY`) for Grok-2.
   * Google Cloud Vertex Project credentials (`GEMINI_PROJECT_ID`) for Gemini.
   *(Note: The system includes a built-in standalone fallback engine that allows complete local demo execution even without external API keys).*

---

### Step 1: Start MongoDB and Redis Databases
From the project root directory, run Docker Compose:

```bash
docker-compose up -d
```

Verify containers are running:
* **MongoDB:** `localhost:27017`
* **Redis:** `localhost:6379`

To verify status:
```bash
docker ps
```

---

### Step 2: Configure Environment Variables (Optional)
Configure your API keys in your environment or directly in `backend/src/main/resources/application.yml`:

**Windows (PowerShell):**
```powershell
$env:XAI_API_KEY="xai-your-api-key-here"
$env:GEMINI_PROJECT_ID="your-gcp-project-id"
```

**Linux / macOS (Bash/Zsh):**
```bash
export XAI_API_KEY="xai-your-api-key-here"
export GEMINI_PROJECT_ID="your-gcp-project-id"
```

---

### Step 3: Compile and Run the Backend Server
Navigate to the `backend` folder and run the Spring Boot application using the Gradle wrapper:

**Windows:**
```powershell
cd backend
.\gradlew.bat bootRun
```

**Linux / macOS:**
```bash
cd backend
chmod +x gradlew
./gradlew bootRun
```

The backend server will start on: **`http://localhost:8080`**

Verify backend health:
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

---

### Step 4: Open the Interactive Frontend Dashboard
The frontend is a lightweight, zero-dependency client that interfaces with the TrAI backend REST API.

1. Open `frontend/index.html` directly in any modern browser:
   * **Windows:** Double-click `frontend\index.html` or run:
     ```powershell
     Start-Process "frontend\index.html"
     ```
   * **macOS:**
     ```bash
     open frontend/index.html
     ```
   * **Linux:**
     ```bash
     xdg-open frontend/index.html
     ```
2. Or serve it via any local static server:
   ```bash
   # Using Python:
   cd frontend
   python -m http.server 3000
   # Then visit http://localhost:3000
   ```

---

## 6. Live Fact-Checking & Video Transcription Plugin Workflow

To answer your specific requirement:
> *"a plugin which can real time transcribe live video, and check it and give some summary that for example Donald Trump says things is correct or wrong and say some true points"*

### How It Operates:
1. **Audio/Video Ingestion:** Live broadcast feeds (YouTube live, RTSP stream, or microphone) are streamed through an audio transcription node (such as Whisper or Google Cloud Speech-to-Text).
2. **Chunking & Claim Extraction:** Spoken sentences are grouped into contextual transcript chunks.
3. **TrAI Verification Pipeline:** The chunk is sent to `POST /api/v1/live/verify-statement`:
   ```json
   {
     "speaker": "Donald Trump",
     "mediaSource": "Live Campaign Speech / C-SPAN",
     "statement": "We put 25 percent tariffs on foreign steel, and our inflation completely disappeared to zero, creating 100 percent of the manufacturing jobs in our country."
   }
   ```
4. **Instant Analytical Output:**
   * **Verdict:** `MISLEADING` / `FALSE`
   * **Truth Score:** `38/100`
   * **Claim Breakdown:**
     * `[VERIFIED]` Tariffs on foreign steel were set at 25% (March 2018 Section 232).
     * `[DEBUNKED]` Inflation did not reach zero; CPI was 2.4% in 2018 and 1.8% in 2019.
     * `[DEBUNKED]` Manufacturing added ~450k jobs, representing a fractional share of total employment.

---

## 7. REST API Documentation for B2B Integration

### 1. Live Statement Fact-Check
* **Endpoint:** `POST /api/v1/live/verify-statement`
* **Request:**
  ```json
  {
    "speaker": "Donald Trump",
    "mediaSource": "Live Presidential Debate",
    "statement": "The entire national debt was paid off during the third quarter."
  }
  ```

### 2. AI Hallucination & Glitch Auditor
* **Endpoint:** `POST /api/v1/trust/verify-ai-output`
* **Request:**
  ```json
  {
    "prompt": "Who signed the 1997 Kyoto Protocol on behalf of the US?",
    "aiResponse": "Signed by President Bill Clinton in Geneva and ratified unanimously by Senate.",
    "modelName": "Internal Enterprise LLM"
  }
  ```

### 3. News Propaganda Stripping
* **Endpoint:** `POST /api/v1/trust/verify-news`
* **Request:**
  ```json
  {
    "sourceName": "Foreign Wire",
    "text": "Hostile forces suffered catastrophic annihilation by our heroic defense units..."
  }
  ```

### 4. Global Source Credibility Index
* **Endpoint:** `GET /api/v1/trust/sources`

---

## 8. Summary of Commercial Pitch Deck (For Client Meetings)

| Slide | Headline | Key Talking Point for B2B Client |
| :--- | :--- | :--- |
| **Slide 1** | **The Epidemic of Misinformation** | Newsrooms, FinTechs, and AI teams face multi-million dollar liability from false claims and AI glitches. |
| **Slide 2** | **Introducing TrAI Truth Gateway** | The world's first automated, neutral Truth-as-a-Service engine combining multi-model AI consensus. |
| **Slide 3** | **Live Video Fact-Check Plugin** | Instant fact verification of live broadcast speeches (e.g. Donald Trump, political candidates, corporate executives) before misinformation spreads. |
| **Slide 4** | **AI Glitch FireWall** | Prevent generative AI hallucinations from reaching your enterprise customers. |
| **Slide 5** | **B2B Commercial Partnership** | Software deployment + 24/7 dedicated support SLA + custom compliance feeds. |
