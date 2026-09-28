package com.antigravity.engine.controller;

import com.antigravity.engine.domain.SourceTrustScore;
import com.antigravity.engine.dto.AiGlitchCheckRequest;
import com.antigravity.engine.dto.LiveStatementRequest;
import com.antigravity.engine.dto.NewsVerificationRequest;
import com.antigravity.engine.service.TrustVerificationService;
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
     * Platform status & engine capability diagnostic.
     */
    @GetMapping("/trust/status")
    public ResponseEntity<Map<String, Object>> getEngineStatus() {
        return ResponseEntity.ok(Map.of(
                "application", "TrAI Trust Engine Platform",
                "version", "0.0.1-SNAPSHOT",
                "status", "OPERATIONAL",
                "features", List.of(
                        "Live Video/Speech Transcription Fact-Checking",
                        "AI Hallucination & Glitch Detection",
                        "Multi-source Consensus Verification",
                        "Propaganda & Double-Standard Stripping",
                        "Source Trust Scoring Index"
                ),
                "supportedModels", List.of("xAI Grok-2", "Google Vertex AI Gemini 1.5 Pro")
        ));
    }
}
