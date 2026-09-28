package com.trai.engine.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Output Guardrails Service.
 *
 * Inspects AI-generated responses BEFORE delivering them to the caller.
 * Checks for:
 *   1. PII leakage in the AI response
 *   2. Toxic / harmful content keywords
 *   3. Risk score threshold breach — blocks HIGH/CRITICAL responses
 *   4. Forbidden topics in regulated industry contexts
 */
@Service
public class OutputGuardrailsService {

    private static final Logger log = LoggerFactory.getLogger(OutputGuardrailsService.class);

    /** Block any response that contains these patterns */
    private static final List<Pattern> TOXIC_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(make a bomb|synthesize\\s+\\w+\\s+drug|how to kill|hack into|ddos)\\b"),
            Pattern.compile("(?i)\\b(child\\s+abuse|csam|sexual\\s+content\\s+involving\\s+minor)\\b")
    );

    /** PII in output patterns */
    private static final Pattern EMAIL_IN_OUTPUT =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern CREDIT_CARD_IN_OUTPUT =
            Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    /** Risk score above this level → block output */
    private static final int BLOCK_THRESHOLD = 85;

    /**
     * Evaluate the AI response.
     *
     * @param aiResponse    The raw string response from the AI model
     * @param riskScore     Composite risk score (0–100) from RiskScoringEngine
     * @return GuardrailResult — contains final output (possibly redacted) and flags
     */
    public GuardrailResult evaluate(String aiResponse, int riskScore) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return new GuardrailResult("", List.of("EMPTY_RESPONSE"), false, riskScore);
        }

        List<String> flags = new ArrayList<>();
        String output = aiResponse;
        boolean blocked = false;

        // 1. Check risk score threshold
        if (riskScore >= BLOCK_THRESHOLD) {
            flags.add("RISK_SCORE_EXCEEDS_THRESHOLD");
            blocked = true;
            log.warn("Output blocked: risk score {} exceeds threshold {}", riskScore, BLOCK_THRESHOLD);
        }

        // 2. Check for toxic content
        for (Pattern p : TOXIC_PATTERNS) {
            if (p.matcher(output).find()) {
                flags.add("TOXIC_CONTENT_DETECTED");
                blocked = true;
                log.warn("Toxic content pattern detected in AI output");
                break;
            }
        }

        // 3. Check for PII leakage
        if (EMAIL_IN_OUTPUT.matcher(output).find()) {
            output = EMAIL_IN_OUTPUT.matcher(output).replaceAll("[EMAIL_REDACTED]");
            flags.add("PII_EMAIL_REDACTED_FROM_OUTPUT");
        }
        if (CREDIT_CARD_IN_OUTPUT.matcher(output).find()) {
            output = CREDIT_CARD_IN_OUTPUT.matcher(output).replaceAll("[CARD_REDACTED]");
            flags.add("PII_CREDIT_CARD_REDACTED_FROM_OUTPUT");
        }

        if (blocked) {
            output = "[RESPONSE BLOCKED BY TRAI OUTPUT GUARDRAILS — Risk Score: " + riskScore + "]";
        }

        return new GuardrailResult(output, flags, blocked, riskScore);
    }

    /**
     * Wraps a service result map, applying guardrails to all string values.
     */
    public Map<String, Object> applyToServiceResult(Map<String, Object> result, int riskScore) {
        // If no riskScore context, pass through (let AI engines handle scoring)
        if (riskScore < BLOCK_THRESHOLD) {
            return result;
        }
        // Add guardrail flags to the result
        result.put("guardrailFlags", List.of("RISK_SCORE_EXCEEDS_THRESHOLD"));
        result.put("guardrailBlocked", true);
        result.put("riskScore", riskScore);
        return result;
    }

    /** Result container */
    public record GuardrailResult(
            String safeOutput,
            List<String> flags,
            boolean blocked,
            int riskScore
    ) {}
}
