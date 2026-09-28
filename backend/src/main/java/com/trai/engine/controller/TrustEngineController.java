package com.trai.engine.controller;

import com.trai.engine.domain.SourceTrustScore;
import com.trai.engine.dto.AiGlitchCheckRequest;
import com.trai.engine.dto.LiveStatementRequest;
import com.trai.engine.dto.NewsVerificationRequest;
import com.trai.engine.service.TrustVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Enterprise B2B Trust-as-a-Service (TaaS) REST API Controller.
 * Provides live fact-checking, AI hallucination audits, and news credibility scoring.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*") // Allows local frontend and external B2B clients
public class TrustEngineController {

    private final TrustVerificationService trustVerificationService;

    public TrustEngineController(TrustVerificationService trustVerificationService) {
        this.trustVerificationService = trustVerificationService;
    }

    /**
     * Real-time Live Video / Speech statement verification endpoint.
     * Transcribed text from live video streams, political debates, or press conferences.
     */
    @PostMapping("/live/verify-statement")
    public ResponseEntity<Map<String, Object>> verifyLiveStatement(@RequestBody LiveStatementRequest request) {
        Map<String, Object> result = trustVerificationService.verifyLiveStatement(request);
        return ResponseEntity.ok(result);
    }

    /**
     * AI Output Hallucination & Glitch Auditor endpoint.
     * Validates whether an LLM's answer is accurate or a hallucination/glitch.
     */
    @PostMapping("/trust/verify-ai-output")
    public ResponseEntity<Map<String, Object>> verifyAiOutput(@RequestBody AiGlitchCheckRequest request) {
        Map<String, Object> result = trustVerificationService.verifyAiGlitch(request);
        return ResponseEntity.ok(result);
    }

    /**
     * News Article Propaganda Stripping & Normalization endpoint.
     * Removes bias, verifies consensus, flags unverified claims.
     */
    @PostMapping("/trust/verify-news")
    public ResponseEntity<Map<String, Object>> verifyNewsArticle(@RequestBody NewsVerificationRequest request) {
        Map<String, Object> result = trustVerificationService.verifyNewsArticle(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Global Source Credibility & Trust Rankings.
     */
    @GetMapping("/trust/sources")
    public ResponseEntity<List<SourceTrustScore>> getSourceTrustScores() {
        List<SourceTrustScore> scores = trustVerificationService.getSourceTrustScores();
        return ResponseEntity.ok(scores);
    }

    /**
     * Algorithmic market impact prediction for verified events.
     */
    @PostMapping("/analytics/market-impact")
    public ResponseEntity<Map<String, Object>> predictMarketImpact(@RequestBody Map<String, String> body) {
        String summary = body.getOrDefault("eventSummary", "");
        Map<String, Object> result = trustVerificationService.predictMarketImpact(summary);
        return ResponseEntity.ok(result);
    }

    /**
     * Get SentinelMind Emergency Kill-Switch status.
     */
    @GetMapping("/trust/kill-switch")
    public ResponseEntity<Map<String, Object>> getKillSwitchStatus() {
        return ResponseEntity.ok(Map.of(
                "active", trustVerificationService.isKillSwitchActive()
        ));
    }

    /**
     * Toggle SentinelMind Emergency Kill-Switch.
     */
    @PostMapping("/trust/kill-switch")
    public ResponseEntity<Map<String, Object>> toggleKillSwitch(@RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", false);
        boolean updated = trustVerificationService.setKillSwitch(active);
        return ResponseEntity.ok(Map.of(
                "active", updated,
                "status", updated ? "KILL_SWITCH_ENGAGED" : "SYSTEM_NORMAL",
                "message", updated ? "Emergency Kill-Switch is now ACTIVE. AI pipelines halted." : "Emergency Kill-Switch DISENGAGED. AI pipelines normal."
        ));
    }

    /**
     * Platform status & engine capability diagnostic.
     */
    @GetMapping("/trust/status")
    public ResponseEntity<Map<String, Object>> getEngineStatus() {
        boolean killActive = trustVerificationService.isKillSwitchActive();
        return ResponseEntity.ok(Map.of(
                "application", "TrAI Trust Engine Platform",
                "version", "0.0.1-SNAPSHOT",
                "status", killActive ? "HALTED_BY_KILL_SWITCH" : "OPERATIONAL",
                "killSwitchActive", killActive,
                "features", List.of(
                        "Live Video/Speech Transcription Fact-Checking",
                        "AI Hallucination & Glitch Detection",
                        "Multi-source Consensus Verification",
                        "Propaganda & Double-Standard Stripping",
                        "Source Trust Scoring Index",
                        "B2B Webhook Gateway",
                        "Predictive Market Impact Analytics",
                        "SentinelMind Emergency Kill Switch"
                ),
                "supportedModels", List.of("xAI Grok-2", "Google Vertex AI Gemini 1.5 Pro")
        ));
    }
}
