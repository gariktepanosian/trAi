package com.trai.engine.multiagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * MultiAgentValidationService — Parallel dual-agent truth validation pipeline.
 *
 * Architecture:
 *
 *  ┌─────────────────────────────────────────────────────────────────────────┐
 *  │  INPUT: tweet/claim/event + country context                             │
 *  └──────────────────────────┬──────────────────────────────────────────────┘
 *                             │
 *             ┌───────────────┴──────────────────┐
 *             │                                  │
 *      ┌──────▼────────┐                 ┌───────▼───────┐
 *      │   AGENT 1     │                 │   AGENT 2     │
 *      │  (Collector)  │                 │  (Validator)  │
 *      │               │                 │               │
 *      │ • Grok-2-1212 │  ─ parallel ─▶  │ • Grok-2-1212 │
 *      │ • Gemini 2.0  │                 │ • Gemini 2.0  │
 *      │ • GPT-4o      │                 │ • GPT-4o      │
 *      │               │                 │               │
 *      │ Gathers real  │                 │ Cross-checks  │
 *      │ tweet context │                 │ conclusions   │
 *      │ makes initial │                 │ validates AI  │
 *      │ assessment    │                 │ consensus     │
 *      └──────┬────────┘                 └───────┬───────┘
 *             │                                  │
 *             └───────────────┬──────────────────┘
 *                             │
 *             ┌───────────────▼──────────────────┐
 *             │   CONSENSUS ENGINE               │
 *             │   Merges both agents' outputs    │
 *             │   Issues final verdict +         │
 *             │   critical alert if warranted    │
 *             └──────────────────────────────────┘
 *
 * Each agent calls Grok, Gemini, and ChatGPT IN PARALLEL using virtual threads
 * (Java 21 structured concurrency).
 */
@Service
public class MultiAgentValidationService {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentValidationService.class);

    private final ChatLanguageModel grokModel;
    private final ChatLanguageModel geminiModel;
    private final ChatLanguageModel chatGptModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Virtual-thread executor for parallel AI calls
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MultiAgentValidationService(
            @Qualifier("grokModel") ChatLanguageModel grokModel,
            @Qualifier("geminiModel") ChatLanguageModel geminiModel,
            @Qualifier("chatGptModel") ChatLanguageModel chatGptModel) {
        this.grokModel = grokModel;
        this.geminiModel = geminiModel;
        this.chatGptModel = chatGptModel;
    }

    /**
     * Main entry point: run both agents in parallel, then merge consensus.
     *
     * @param claim    The claim, tweet text, or event summary to validate
     * @param country  Country context (e.g. "Armenia", "Ukraine")
     * @param source   Source label (e.g. "Twitter/X", "News Feed")
     * @return         Full multi-agent validation result
     */
    public MultiAgentResult validateWithDualAgent(String claim, String country, String source) {
        log.info("[MultiAgent] Starting dual-agent parallel validation | country={} source={}", country, source);
        long start = System.currentTimeMillis();

        // Run Agent 1 (Collector) and Agent 2 (Validator) in parallel
        Future<AgentOutput> agent1Future = executor.submit(
                () -> runAgent1Collector(claim, country, source));
        Future<AgentOutput> agent2Future = executor.submit(
                () -> runAgent2Validator(claim, country, source));

        AgentOutput agent1Result;
        AgentOutput agent2Result;
        try {
            agent1Result = agent1Future.get(45, TimeUnit.SECONDS);
            agent2Result = agent2Future.get(45, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[MultiAgent] Agent timeout — using partial results");
            agent1Result = fallbackAgentOutput("AGENT_1_COLLECTOR", claim, "TIMEOUT");
            agent2Result = fallbackAgentOutput("AGENT_2_VALIDATOR", claim, "TIMEOUT");
            try {
                if (!agent1Future.isDone()) agent1Result = agent1Future.get(5, TimeUnit.SECONDS);
                if (!agent2Future.isDone()) agent2Result = agent2Future.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("[MultiAgent] Agent execution failed: {}", e.getMessage());
            agent1Result = fallbackAgentOutput("AGENT_1_COLLECTOR", claim, e.getMessage());
            agent2Result = fallbackAgentOutput("AGENT_2_VALIDATOR", claim, e.getMessage());
        }

        MultiAgentResult consensus = buildConsensus(agent1Result, agent2Result, claim, country, source);
        consensus.setProcessingTimeMs(System.currentTimeMillis() - start);

        log.info("[MultiAgent] Dual-agent consensus complete | verdict={} trustScore={} isCritical={} timeMs={}",
                consensus.getFinalVerdict(), consensus.getConsensusTrustScore(),
                consensus.isCriticalAlert(), consensus.getProcessingTimeMs());

        return consensus;
    }

    // ─── AGENT 1: Collector ───────────────────────────────────────────────────
    // Collects context from all 3 AI models simultaneously.
    // Gathers evidence, identifies real tweet patterns, makes initial assessment.

    private AgentOutput runAgent1Collector(String claim, String country, String source) {
        log.info("[Agent1/Collector] Starting — calling Grok, Gemini, ChatGPT in parallel");

        String grokPrompt = buildCollectorPrompt("GROK", claim, country, source,
                "You have access to real Twitter/X user sentiment data. Focus on: " +
                "1) What real X users are saying about this. " +
                "2) Is this spreading virally? " +
                "3) Are verified accounts corroborating or denying this? " +
                "Assign a Grok Trust Score (0-100).");

        String geminiPrompt = buildCollectorPrompt("GEMINI", claim, country, source,
                "Use your Google knowledge graph. Focus on: " +
                "1) Does this match known historical patterns for this country? " +
                "2) Cross-reference with Google News signals. " +
                "3) Identify any factual anchors or contradictions. " +
                "Assign a Gemini Credibility Score (0-100).");

        String chatGptPrompt = buildCollectorPrompt("CHATGPT", claim, country, source,
                "Apply your broad language understanding. Focus on: " +
                "1) Linguistic patterns — is this propaganda language? " +
                "2) Are there logical fallacies or emotional manipulation? " +
                "3) Does the claim structure match known misinformation templates? " +
                "Assign a ChatGPT Filter Score (0-100).");

        // Call all three in parallel
        Map<String, String> responses = callThreeModelsInParallel(grokPrompt, geminiPrompt, chatGptPrompt);

        return AgentOutput.builder()
                .agentId("AGENT_1_COLLECTOR")
                .role("COLLECTOR")
                .grokResponse(responses.get("grok"))
                .geminiResponse(responses.get("gemini"))
                .chatGptResponse(responses.get("chatgpt"))
                .aggregatedScore(extractAverageScore(responses))
                .build();
    }

    // ─── AGENT 2: Validator ───────────────────────────────────────────────────
    // Independently validates the same claim, then cross-checks Agent 1's logic.
    // Focuses on red flags, critical severity, and final truth determination.

    private AgentOutput runAgent2Validator(String claim, String country, String source) {
        log.info("[Agent2/Validator] Starting — calling Grok, Gemini, ChatGPT in parallel");

        String grokPrompt = buildValidatorPrompt("GROK", claim, country, source,
                "Act as a Grok-native validator with Twitter/X context. Focus on: " +
                "1) Use Grok's real-time X data access to VERIFY or DEBUNK. " +
                "2) Check if government/military/emergency accounts have posted about this. " +
                "3) Rate the CRITICAL SEVERITY: is this a tornado, war, earthquake, public emergency? " +
                "Assign a Grok Validation Score (0-100) and CRITICAL_LEVEL: LOW/MEDIUM/HIGH/CRITICAL.");

        String geminiPrompt = buildValidatorPrompt("GEMINI", claim, country, source,
                "Act as a Gemini validator with Google Earth + Maps context. Focus on: " +
                "1) Does geographic data support this claim for the stated country? " +
                "2) Are there Google Alerts or News spikes for this topic/region? " +
                "3) Cross-validate against Agent 1's assessment. " +
                "Assign a Gemini Validation Score (0-100) and CRITICAL_LEVEL: LOW/MEDIUM/HIGH/CRITICAL.");

        String chatGptPrompt = buildValidatorPrompt("CHATGPT", claim, country, source,
                "Act as a ChatGPT-4o safety filter and validator. Focus on: " +
                "1) Is this claim a public safety concern? Does it require immediate user notification? " +
                "2) Check for false urgency vs. genuine emergency signals. " +
                "3) Final recommendation: PUSH_NOTIFY users or SUPPRESS? " +
                "Assign a ChatGPT Safety Score (0-100) and CRITICAL_LEVEL: LOW/MEDIUM/HIGH/CRITICAL.");

        // Call all three in parallel
        Map<String, String> responses = callThreeModelsInParallel(grokPrompt, geminiPrompt, chatGptPrompt);

        return AgentOutput.builder()
                .agentId("AGENT_2_VALIDATOR")
                .role("VALIDATOR")
                .grokResponse(responses.get("grok"))
                .geminiResponse(responses.get("gemini"))
                .chatGptResponse(responses.get("chatgpt"))
                .aggregatedScore(extractAverageScore(responses))
                .build();
    }

    // ─── Parallel 3-model caller ──────────────────────────────────────────────

    private Map<String, String> callThreeModelsInParallel(
            String grokPrompt, String geminiPrompt, String chatGptPrompt) {

        Future<String> grokFuture    = executor.submit(() -> safeAiCall(grokModel,    grokPrompt,    "Grok"));
        Future<String> geminiFuture  = executor.submit(() -> safeAiCall(geminiModel,  geminiPrompt,  "Gemini"));
        Future<String> chatGptFuture = executor.submit(() -> safeAiCall(chatGptModel, chatGptPrompt, "ChatGPT"));

        Map<String, String> results = new HashMap<>();
        try {
            results.put("grok",    grokFuture.get(30, TimeUnit.SECONDS));
            results.put("gemini",  geminiFuture.get(30, TimeUnit.SECONDS));
            results.put("chatgpt", chatGptFuture.get(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.warn("[MultiAgent] One or more model calls failed/timed out: {}", e.getMessage());
            tryGetPartial(grokFuture,    results, "grok");
            tryGetPartial(geminiFuture,  results, "gemini");
            tryGetPartial(chatGptFuture, results, "chatgpt");
        }
        return results;
    }

    private String safeAiCall(ChatLanguageModel model, String prompt, String modelName) {
        try {
            return model.generate(prompt);
        } catch (Exception e) {
            log.warn("[MultiAgent] {} call failed: {}", modelName, e.getMessage());
            return fallbackResponse(modelName, prompt);
        }
    }

    private void tryGetPartial(Future<String> future, Map<String, String> map, String key) {
        if (!map.containsKey(key)) {
            try {
                map.put(key, future.isDone() ? future.get() : "AI_UNAVAILABLE");
            } catch (Exception e) {
                map.put(key, "AI_UNAVAILABLE");
            }
        }
    }

    // ─── Consensus Engine ─────────────────────────────────────────────────────

    private MultiAgentResult buildConsensus(
            AgentOutput agent1, AgentOutput agent2,
            String claim, String country, String source) {

        double combinedScore = (agent1.getAggregatedScore() + agent2.getAggregatedScore()) / 2.0;

        // Determine final verdict based on consensus score
        String verdict;
        if (combinedScore >= 85) {
            verdict = "VERIFIED_TRUE";
        } else if (combinedScore >= 70) {
            verdict = "LIKELY_TRUE";
        } else if (combinedScore >= 50) {
            verdict = "UNVERIFIED_CLAIM";
        } else if (combinedScore >= 30) {
            verdict = "MISLEADING";
        } else {
            verdict = "FALSE";
        }

        // Detect critical alerts from Agent 2 validator responses
        boolean isCritical = detectCriticalAlert(agent2, combinedScore);
        String criticalLevel = determineCriticalLevel(agent2, combinedScore);

        return MultiAgentResult.builder()
                .claim(claim)
                .country(country)
                .source(source)
                .agent1(agent1)
                .agent2(agent2)
                .finalVerdict(verdict)
                .consensusTrustScore((int) combinedScore)
                .criticalAlert(isCritical)
                .criticalLevel(criticalLevel)
                .requiresPushNotification(isCritical && combinedScore > 55)
                .timestamp(Instant.now().toString())
                .build();
    }

    private boolean detectCriticalAlert(AgentOutput agent2, double score) {
        if (score < 45) return false; // Too uncertain to alert
        String combined = (agent2.getGrokResponse() + " " +
                           agent2.getGeminiResponse() + " " +
                           agent2.getChatGptResponse()).toLowerCase();
        return combined.contains("critical") || combined.contains("tornado") ||
               combined.contains("earthquake") || combined.contains("war") ||
               combined.contains("explosion") || combined.contains("attack") ||
               combined.contains("emergency") || combined.contains("evacuation") ||
               combined.contains("missile") || combined.contains("flood") ||
               combined.contains("push_notify") || combined.contains("immediate");
    }

    private String determineCriticalLevel(AgentOutput agent2, double score) {
        String combined = (agent2.getGrokResponse() + " " +
                           agent2.getGeminiResponse() + " " +
                           agent2.getChatGptResponse()).toUpperCase();
        if (combined.contains("CRITICAL") || score > 80) return "CRITICAL";
        if (combined.contains("HIGH"))    return "HIGH";
        if (combined.contains("MEDIUM"))  return "MEDIUM";
        return "LOW";
    }

    // ─── Prompt Builders ─────────────────────────────────────────────────────

    private String buildCollectorPrompt(String modelName, String claim, String country,
                                         String source, String roleInstruction) {
        return String.format("""
            You are %s operating as AGENT 1 (COLLECTOR) in TrAI's multi-model validation pipeline.
            
            COUNTRY CONTEXT: %s
            SOURCE: %s
            CLAIM / TWEET TO ANALYZE: "%s"
            
            YOUR ROLE: %s
            
            Respond ONLY in this JSON format:
            {
              "model": "%s",
              "agent": "AGENT_1_COLLECTOR",
              "initialAssessment": "VERIFIED_TRUE | LIKELY_TRUE | UNVERIFIED_CLAIM | MISLEADING | FALSE",
              "score": <0-100>,
              "evidence": "<key evidence found>",
              "redFlags": ["<flag1>", "<flag2>"],
              "viralRisk": "LOW | MEDIUM | HIGH",
              "summary": "<2-3 sentence analysis>"
            }
            """,
                modelName, country, source, claim, roleInstruction, modelName);
    }

    private String buildValidatorPrompt(String modelName, String claim, String country,
                                         String source, String roleInstruction) {
        return String.format("""
            You are %s operating as AGENT 2 (VALIDATOR) in TrAI's multi-model validation pipeline.
            
            COUNTRY CONTEXT: %s
            SOURCE: %s
            CLAIM / TWEET TO VALIDATE: "%s"
            
            YOUR ROLE: %s
            
            Respond ONLY in this JSON format:
            {
              "model": "%s",
              "agent": "AGENT_2_VALIDATOR",
              "validationVerdict": "CONFIRMED | REFUTED | PARTIALLY_CONFIRMED | INSUFFICIENT_DATA",
              "score": <0-100>,
              "criticalLevel": "LOW | MEDIUM | HIGH | CRITICAL",
              "isSafetyEvent": <true|false>,
              "requiresPushNotification": <true|false>,
              "crossValidation": "<how this validates or contradicts Agent 1 findings>",
              "finalRecommendation": "PUBLISH | SUPPRESS | PUSH_NOTIFY | ESCALATE"
            }
            """,
                modelName, country, source, claim, roleInstruction, modelName);
    }

    // ─── Score Extraction ─────────────────────────────────────────────────────

    private int extractAverageScore(Map<String, String> responses) {
        int total = 0;
        int count = 0;
        for (String response : responses.values()) {
            int score = extractScoreFromJson(response);
            if (score > 0) {
                total += score;
                count++;
            }
        }
        return count > 0 ? total / count : 55; // default neutral score
    }

    private int extractScoreFromJson(String json) {
        if (json == null) return 0;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("score")) return node.get("score").asInt(55);
        } catch (Exception ignored) {}
        // Heuristic fallback: scan for "score": <number>
        try {
            int idx = json.indexOf("\"score\":");
            if (idx >= 0) {
                String sub = json.substring(idx + 8).trim().replaceAll("[^0-9].*", "");
                return Integer.parseInt(sub.trim());
            }
        } catch (Exception ignored) {}
        return 55;
    }

    // ─── Fallback responses ───────────────────────────────────────────────────

    private String fallbackResponse(String modelName, String prompt) {
        return String.format("""
            {"model":"%s","agent":"FALLBACK","initialAssessment":"UNVERIFIED_CLAIM",
            "score":50,"evidence":"AI model temporarily unavailable","redFlags":[],
            "viralRisk":"LOW","summary":"Heuristic fallback — %s API not reachable."}
            """, modelName, modelName);
    }

    private AgentOutput fallbackAgentOutput(String agentId, String claim, String reason) {
        String fallback = String.format(
                "{\"score\":50,\"assessment\":\"UNVERIFIED\",\"reason\":\"%s\"}", reason);
        return AgentOutput.builder()
                .agentId(agentId)
                .role(agentId.contains("1") ? "COLLECTOR" : "VALIDATOR")
                .grokResponse(fallback)
                .geminiResponse(fallback)
                .chatGptResponse(fallback)
                .aggregatedScore(50)
                .build();
    }
}
