# SKILL: Multi-Agent Parallel Validation Pipeline
# TrAI Platform — Dual-Agent Consensus Engine

## What this skill covers

The multi-agent system runs **two independent agents in parallel**, each calling
**all three AI models simultaneously** (Grok + Gemini + ChatGPT = 6 parallel AI calls total).
This skill covers architecture, data flow, adding new agents, and extending the pipeline.

---

## File Locations

```
backend/src/main/java/com/trai/engine/multiagent/
├── MultiAgentValidationService.java   # Main orchestrator
├── AgentOutput.java                   # Single agent result (all 3 model responses)
└── MultiAgentResult.java              # Final consensus result

REST endpoint: POST /api/v1/validate/multi-agent
Defined in:    TrustEngineController.java
```

---

## Architecture

```
Input: claim (String) + country (String) + source (String)
            │
   ┌─────────┴──────────┐    ← Both submitted to ExecutorService simultaneously
   │                    │
AGENT 1 (Collector)   AGENT 2 (Validator)
   │                    │
   ├── Grok call        ├── Grok call        ← All 6 calls run via virtual threads
   ├── Gemini call      ├── Gemini call         Java 21: newVirtualThreadPerTaskExecutor()
   └── ChatGPT call     └── ChatGPT call
   │                    │
   └──────────┬─────────┘
              │
    CONSENSUS ENGINE
    ├── Average scores from both agents
    ├── Determine final verdict (VERIFIED_TRUE → FALSE scale)
    ├── Detect critical alert (keyword + score threshold)
    ├── Set criticalLevel (LOW/MEDIUM/HIGH/CRITICAL)
    └── Set requiresPushNotification (isCritical && score > 55)
              │
       MultiAgentResult
```

---

## Data Classes

### `AgentOutput`
Holds the three model responses for a single agent.

```java
AgentOutput {
    String agentId;           // "AGENT_1_COLLECTOR" or "AGENT_2_VALIDATOR"
    String role;              // "COLLECTOR" or "VALIDATOR"
    String grokResponse;      // Raw JSON string from Grok
    String geminiResponse;    // Raw JSON string from Gemini
    String chatGptResponse;   // Raw JSON string from ChatGPT
    int aggregatedScore;      // Average of parsed scores from all 3 responses
}
```

### `MultiAgentResult`
The final output returned to the caller.

```java
MultiAgentResult {
    String claim;                    // The input claim
    String country;                  // Country context
    String source;                   // e.g. "Twitter/X"
    AgentOutput agent1;              // Full Agent 1 output
    AgentOutput agent2;              // Full Agent 2 output
    String finalVerdict;             // VERIFIED_TRUE | LIKELY_TRUE | UNVERIFIED_CLAIM | MISLEADING | FALSE
    int consensusTrustScore;         // 0-100 combined score
    boolean criticalAlert;           // true = push notification should fire
    String criticalLevel;            // LOW | MEDIUM | HIGH | CRITICAL
    boolean requiresPushNotification; // final push decision
    String timestamp;                // ISO-8601
    long processingTimeMs;           // total wall-clock time
}
```

---

## Verdict Scale

| consensusTrustScore | finalVerdict |
|---|---|
| >= 85 | `VERIFIED_TRUE` |
| >= 70 | `LIKELY_TRUE` |
| >= 50 | `UNVERIFIED_CLAIM` |
| >= 30 | `MISLEADING` |
| < 30 | `FALSE` |

---

## Critical Alert Detection

An alert fires when **both** conditions are true:
1. Agent 2's combined responses contain critical keywords:
   `critical`, `tornado`, `earthquake`, `war`, `explosion`, `attack`, `emergency`,
   `evacuation`, `missile`, `flood`, `push_notify`, `immediate`
2. `consensusTrustScore >= 55` (enough confidence to avoid false alarms)

If the score is < 45, no alert fires regardless of keywords (too uncertain).

---

## Agent Role Prompts

### Agent 1 — Collector
Each of the 3 model prompts includes a model-specific perspective:
- **Grok:** Focus on real Twitter/X user sentiment, virality, verified accounts
- **Gemini:** Focus on Google knowledge graph, News signals, geographic facts
- **ChatGPT:** Focus on linguistic propaganda patterns, logical fallacies, misinformation templates

### Agent 2 — Validator
Each prompt asks for cross-validation with a decision:
- **Grok:** Confirm/deny using Grok's real-time X data; CRITICAL_LEVEL rating
- **Gemini:** Geographic validation; cross-validate Agent 1 findings
- **ChatGPT:** Safety filter; `requiresPushNotification` true/false; final recommendation

---

## Expected JSON Formats

### Agent 1 (Collector) model response JSON
```json
{
  "model": "GROK",
  "agent": "AGENT_1_COLLECTOR",
  "initialAssessment": "VERIFIED_TRUE",
  "score": 82,
  "evidence": "Multiple verified X accounts reporting...",
  "redFlags": [],
  "viralRisk": "HIGH",
  "summary": "Two-sentence analysis..."
}
```

### Agent 2 (Validator) model response JSON
```json
{
  "model": "GEMINI",
  "agent": "AGENT_2_VALIDATOR",
  "validationVerdict": "CONFIRMED",
  "score": 79,
  "criticalLevel": "HIGH",
  "isSafetyEvent": true,
  "requiresPushNotification": true,
  "crossValidation": "Consistent with Agent 1 findings...",
  "finalRecommendation": "PUSH_NOTIFY"
}
```

---

## Score Extraction Strategy

Scores are extracted from model responses using Jackson `readTree()`:
1. Try `node.get("score").asInt(55)` — standard field
2. If JSON parse fails, fall back to regex scan for `"score": <number>`
3. Default: `55` (neutral — does not trigger alerts)

If fewer than 3 model responses return a score, the average uses only available scores.

---

## Adding a Third Agent

To add Agent 3 to the pipeline:

1. Add `Future<AgentOutput> agent3Future` in `validateWithDualAgent()`
2. Create `runAgent3<Role>()` method following the same pattern as Agent 1/2
3. Update `buildConsensus()` to include `agent3.getAggregatedScore()` in the average
4. Add `agent3` field to `MultiAgentResult`
5. Update `SKILL-multi-agent-pipeline.md` (this file) with the new agent's role
6. Update the Playwright test `tests/api/ai-multi-agent.spec.ts` to assert `agent3` in response

---

## Timeout Handling

- Each agent future has a **45-second timeout**
- Each individual model call within an agent has a **30-second timeout**
- On timeout: log warning, use fallback output with score=50 (neutral)
- The app never blocks indefinitely — timeouts are enforced at both levels

---

## Thread Model

Uses Java 21 **virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`).
This means 6+ concurrent AI HTTP calls do not block OS threads.
Do NOT use `@Async` or `CompletableFuture` for new parallel work in this system —
always use the existing `executor` bean from `MultiAgentValidationService`.

---

## Playwright Test Coverage Trigger

Any change to `MultiAgentValidationService`, `AgentOutput`, or `MultiAgentResult`:

- `tests/api/ai-multi-agent.spec.ts` MUST be updated
- Test must assert:
  - Response contains `agent1` and `agent2` objects
  - Response contains `finalVerdict` matching the score scale
  - Response contains `consensusTrustScore` as integer 0-100
  - Response contains `criticalAlert` boolean
  - Response contains `processingTimeMs` > 0
  - When mocked AI returns critical keywords: `criticalAlert = true`, `requiresPushNotification = true`
  - When mocked AI returns low scores: `criticalAlert = false`
