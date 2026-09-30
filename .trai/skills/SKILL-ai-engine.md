# SKILL: AI Engine
# TrAI Platform — Grok-4.7 + Gemini 2.5 Flash + GPT-4o via LangChain4j

## What this skill covers

All AI model configuration, LangChain4j AiServices wiring, prompt engineering patterns,
and rules for adding new AI-powered features to the TrAI backend.

---

## Active AI Models (v3.0)

| Bean Name | Model | Provider | Base URL | Primary Use |
|---|---|---|---|---|
| `grokModel` (`@Primary`) | `grok-4.7` | xAI | `https://api.x.ai/v1` | All single-model AI features; X-native context; X user authority scoring |
| `geminiModel` | `gemini-2.5-flash` | Google Vertex AI | Vertex AI endpoint | Google knowledge graph; geographic validation; X user cross-validation |
| `chatGptModel` | `gpt-4o` | OpenAI | `https://api.openai.com/v1` | Safety filter; propaganda detection; push notify decision |

**Critical rule:** All model names are **pinned** to specific stable versions.
- Grok: `grok-4.7` (do NOT use `grok-4` or `grok-4-latest` rolling aliases)
- Gemini: `gemini-2.5-flash` (do NOT use `gemini-latest` or experimental suffixes)
- Same rule: always pin a specific version, never use "latest" aliases in production.

---

## Config Location

```
backend/src/main/java/com/trai/engine/config/AiConfig.java
backend/src/main/resources/application.yml        (dev defaults)
backend/src/main/resources/application-prod.yml   (env var refs for production)
```

### `application.yml` AI section
```yaml
langchain4j:
  vertex-ai:
    gemini:
      project: ${GEMINI_PROJECT_ID:your-project-id}
      location: ${GEMINI_LOCATION:us-central1}
      model-name: gemini-2.5-flash          # PINNED — do not change to "latest"

xai:
  api-key: ${XAI_API_KEY:your-xai-api-key}
  model: grok-4.7                            # PINNED

openai:
  api-key: ${OPENAI_API_KEY:your-openai-api-key}
  model: gpt-4o                              # PINNED
```

---

## How LangChain4j AiServices Work

LangChain4j `AiServices` turns a Java interface into an AI-powered proxy.
The interface method parameters become the prompt variables via `@V("name")`.
The `@SystemMessage` annotation sets the fixed system prompt.
The return type is always `String` (raw JSON output from the AI).

### Pattern for a new AI Engine interface

```java
// 1. Define interface in ai/ package
package com.trai.engine.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MyNewEngine {

    @SystemMessage("""
        You are TrAI's <role description>.
        Rules:
        - <rule 1>
        - <rule 2>
        - ALWAYS return valid JSON, no markdown blocks
        """)
    @UserMessage("""
        Input: {{inputVar}}
        Context: {{contextVar}}
        """)
    String analyzeInput(@V("inputVar") String input, @V("contextVar") String context);
}

// 2. Register bean in AiConfig.java
@Bean
public MyNewEngine myNewEngine(ChatLanguageModel grokModel) {  // inject the model you want
    return AiServices.builder(MyNewEngine.class)
            .chatLanguageModel(grokModel)
            .build();
}

// 3. Inject in service via constructor injection
public class MyService {
    private final MyNewEngine myNewEngine;
    public MyService(MyNewEngine myNewEngine) { this.myNewEngine = myNewEngine; }
}
```

---

## Existing AI Engine Interfaces

### `AntiPropagandaEngine` — `ai/AntiPropagandaEngine.java`
- **Method:** `normalizeNewsData(@V("scrapedData") String scrapedData)`
- **Output fields:** `normalizedSummary`, `headline`, `trustScore` (0-100), `propagandaScore` (0-100), `verifiedFacts[]`, `unverifiedClaims[]`
- **Used by:** `TrustVerificationService.verifyNewsArticle()`, `NewsScrapingScheduler`
- **Wired to:** `grokModel` (primary)

### `LiveFactCheckEngine` — `ai/LiveFactCheckEngine.java`
- **Method:** `analyzeLiveTranscript(@V("speaker") String speaker, @V("transcriptChunk") String transcriptChunk)`
- **Output fields:** `speaker`, `statementAnalyzed`, `verdict` (VERIFIED_TRUE/FALSE/MISLEADING/UNVERIFIED_CLAIM), `trustScore` (0-100), `summary`, `factCheckDetails`, `keyClaims[]`, `verifiedDataPoints[]`
- **Used by:** `TrustVerificationService.verifyLiveStatement()`
- **Wired to:** `grokModel`

### `AiGlitchVerifierEngine` — `ai/AiGlitchVerifierEngine.java`
- **Method:** `auditAiResponse(@V("prompt") String prompt, @V("aiResponse") String aiResponse)`
- **Output fields:** `isGlitchDetected`, `glitchSeverity` (NONE/LOW/MODERATE/HIGH), `reliabilityScore` (0-100), `verdictSummary`, `detectedHallucinations[]`, `safeToPublish`
- **Used by:** `TrustVerificationService.verifyAiGlitch()`
- **Wired to:** `grokModel`

### `MarketImpactEngine` — `analytics/MarketImpactEngine.java`
- **Method:** `predictMarketImpact(@V("eventSummary") String eventSummary)`
- **Output fields:** `direction` (UP/DOWN/NEUTRAL), `magnitude` (LOW/MEDIUM/HIGH), `confidence` (0.0-1.0) per asset (Gold/BTC/Oil/USD), `keyFactors[]`, `timeframe`, `caveat`
- **Used by:** `PredictiveAnalyticsService`
- **Wired to:** `grokModel`

---

## X User Authority Ranking (Direct AI Calls)

`XUserRankingService` and `SourceAuthorityService` call the AI models directly (not via
AiServices interfaces) using `ChatLanguageModel.generate(prompt)` because they need
fine-grained control over per-user/per-source prompt construction.

### `XUserRankingService` — `twitter/XUserRankingService.java`
- **Purpose:** Ranks X (formerly Twitter) users by authority and propaganda signals
- **AI models used:** `grokModel` (60% weight) + `geminiModel` (40% weight) in parallel
- **Grok prompt:** Evaluates user credibility, X-native signals, propaganda patterns
- **Gemini prompt:** Cross-validates against Google knowledge graph
- **Output fields in AI response:**
  - `authorityScore` (0–100): overall authority rating
  - `propagandaPenalty` (0–50): penalty for detected propaganda/misinformation
  - `accountType`: OFFICIAL | JOURNALIST | EXPERT | MEDIA_OUTLET | INFLUENCER | UNKNOWN
  - `detectedIssues[]`: PROPAGANDA | EMOTIONAL_MANIPULATION | MISINFORMATION | FALSE_URGENCY
- **Persisted to:** MongoDB `x_user_rank_scores` collection
- **Used by:** `TwitterApiService.parsePosts()` — authorityScore enriches viralityWeight

### `SourceAuthorityService` — `service/SourceAuthorityService.java`
- **Purpose:** Validates news source authority using Grok + Gemini
- **AI models used:** `grokModel` (60%) + `geminiModel` (40%) in parallel
- **Output fields in AI response:**
  - `authorityScore` (0–100): source credibility
  - `propagandaPenalty` (0–50): accumulated penalty
  - `sourceCategory`: TIER_1_INSTITUTIONAL | TIER_2_REPUTABLE | TIER_3_MIXED | ...
  - `detectedIssues[]`: PROPAGANDA | SELECTIVE_REPORTING | KNOWN_MISINFORMATION_SOURCE
- **Persisted to:** MongoDB `source_trust_scores` collection
- **Also called by:** `SourceAuthorityService.recordArticleVerdict()` to track accuracy ratio

---

## Prompt Engineering Rules

1. **Always demand JSON output** — end every system prompt with `"ALWAYS return valid JSON only. No markdown code blocks. No explanations outside JSON."`
2. **Define output schema in the prompt** — list every expected field and its type/enum values
3. **Include a cold/analytical tone instruction** for news/fact-check engines — prevents emotional hedging
4. **Use `@V` for all dynamic inputs** — never string-concatenate into prompts (injection risk)
5. **Keep system prompts under 500 tokens** — longer prompts increase latency and cost

---

## Fallback Pattern (Required for Every AI Call)

Every call to an AI engine must be wrapped in `try/catch` with a deterministic fallback:

```java
String aiOutput;
try {
    aiOutput = myEngine.analyzeInput(input, context);
} catch (Exception e) {
    log.warn("[MyFeature] AI engine unavailable. Using fallback. Cause: {}", e.getMessage());
    aiOutput = buildFallbackOutput(input);  // deterministic heuristic result
}
```

Fallback methods are always named `fallback<MethodPurpose>()` and return the same
Map/String structure as the live AI path.

---

## JSON Output Parsing

AI engines return raw JSON strings. Parse them safely:

```java
private int extractScore(String json, String fieldName, int defaultValue) {
    if (json == null) return defaultValue;
    try {
        JsonNode node = objectMapper.readTree(json);
        if (node.has(fieldName)) return node.get(fieldName).asInt(defaultValue);
    } catch (Exception ignored) {}
    return defaultValue;
}
```

Never use raw string parsing (`.contains()`, `indexOf()`) to extract JSON values.
Always use Jackson `objectMapper.readTree()`.

---

## Adding a New AI Model

If a new AI provider needs to be added (e.g., Anthropic Claude, Meta Llama):

1. Add the LangChain4j dependency to `backend/build.gradle`
2. Add API key env var to `.env.example` with clear comment
3. Add config to `application.yml` and `application-prod.yml`
4. Add `@Value` fields and a new `@Bean` in `AiConfig.java`
5. Name the bean `<modelName>Model` (e.g., `claudeModel`)
6. Update `MultiAgentValidationService` if the new model should join the parallel pipeline
7. Update `SKILL-multi-agent-pipeline.md` to document the new model's role
8. Write a Playwright test for any new endpoint exposed

---

## Environment Variables

| Variable | Bean | Default (dev) |
|---|---|---|
| `XAI_API_KEY` | `grokModel` | `your-xai-api-key` (demo mode) |
| `GEMINI_PROJECT_ID` | `geminiModel` | `your-project-id` (falls back to Grok) |
| `GEMINI_LOCATION` | `geminiModel` | `us-central1` |
| `OPENAI_API_KEY` | `chatGptModel` | `your-openai-api-key` (demo mode) |

**Demo mode behavior:** When any API key is missing/default, the model bean is created
but will fail on first call, which triggers the fallback heuristic. The app always
stays functional without real API keys.

---

## Playwright Test Coverage Trigger

After any change to AI engine interfaces or `AiConfig.java`:

- `tests/api/ai-live-factcheck.spec.ts` — test `POST /api/v1/live/verify-statement` happy + fallback
- `tests/api/ai-news-normalize.spec.ts` — test `POST /api/v1/trust/verify-news`
- `tests/api/ai-glitch-audit.spec.ts` — test `POST /api/v1/trust/verify-ai-output`
- `tests/api/ai-multi-agent.spec.ts` — test `POST /api/v1/validate/multi-agent`
- Mock AI responses using the Playwright `route()` interceptor on `https://api.x.ai/v1` etc.
