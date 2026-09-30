package com.trai.engine.controller;

import com.trai.engine.domain.NormalizedNews;
import com.trai.engine.domain.SourceTrustScore;
import com.trai.engine.dto.AiGlitchCheckRequest;
import com.trai.engine.dto.LiveStatementRequest;
import com.trai.engine.dto.NewsVerificationRequest;
import com.trai.engine.multiagent.MultiAgentResult;
import com.trai.engine.multiagent.MultiAgentValidationService;
import com.trai.engine.repository.NormalizedNewsRepository;
import com.trai.engine.service.TrustVerificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Enterprise B2B Trust-as-a-Service (TaaS) REST API Controller.
 * Provides live fact-checking, AI hallucination audits, and news credibility scoring.
 * v2.0: Added multi-agent validation endpoint + news feed endpoint.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*") // Allows local frontend and external B2B clients
public class TrustEngineController {

    private final TrustVerificationService trustVerificationService;
    private final MultiAgentValidationService multiAgentValidationService;
    private final NormalizedNewsRepository normalizedNewsRepository;

    public TrustEngineController(TrustVerificationService trustVerificationService,
                                  MultiAgentValidationService multiAgentValidationService,
                                  NormalizedNewsRepository normalizedNewsRepository) {
        this.trustVerificationService = trustVerificationService;
        this.multiAgentValidationService = multiAgentValidationService;
        this.normalizedNewsRepository = normalizedNewsRepository;
    }

    /**
     * Multi-Agent Parallel Validation — Grok + Gemini + ChatGPT dual-agent pipeline.
     * Called by Flutter app from Settings → "Check Now" button.
     */
    @PostMapping("/validate/multi-agent")
    public ResponseEntity<MultiAgentResult> multiAgentValidate(@RequestBody Map<String, String> body) {
        String claim   = body.getOrDefault("claim", "");
        String country = body.getOrDefault("country", "Unknown");
        String source  = body.getOrDefault("source", "Manual Input");
        MultiAgentResult result = multiAgentValidationService.validateWithDualAgent(claim, country, source);
        return ResponseEntity.ok(result);
    }

    /**
     * News feed — returns AI-normalized news articles from MongoDB.
     * Optional: filter by country keyword.
     */
    @GetMapping("/news")
    public ResponseEntity<List<NormalizedNews>> getNews(
            @RequestParam(required = false) String country) {
        List<NormalizedNews> news;
        try {
            if (country != null && !country.isBlank()) {
                news = normalizedNewsRepository.findBySourceNameContainingIgnoreCaseOrderByCreatedAtDesc(country);
            } else {
                news = normalizedNewsRepository.findTop20ByOrderByCreatedAtDesc();
            }
        } catch (Exception e) {
            news = List.of();
        }
        return ResponseEntity.ok(news);
    }

    /**
     * Real-time Live Video / Speech statement verification endpoint.
     */
    @PostMapping("/live/verify-statement")
    public ResponseEntity<Map<String, Object>> verifyLiveStatement(@RequestBody LiveStatementRequest request) {
        Map<String, Object> result = trustVerificationService.verifyLiveStatement(request);
        return ResponseEntity.ok(result);
    }

    /**
     * AI Output Hallucination & Glitch Auditor endpoint.
     */
    @PostMapping("/trust/verify-ai-output")
    public ResponseEntity<Map<String, Object>> verifyAiOutput(@RequestBody AiGlitchCheckRequest request) {
        Map<String, Object> result = trustVerificationService.verifyAiGlitch(request);
        return ResponseEntity.ok(result);
    }

    /**
     * News Article Propaganda Stripping & Normalization endpoint.
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
        return ResponseEntity.ok(Map.of("active", trustVerificationService.isKillSwitchActive()));
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
                "message", updated
                        ? "Emergency Kill-Switch is now ACTIVE. AI pipelines halted."
                        : "Emergency Kill-Switch DISENGAGED. AI pipelines normal."
        ));
    }

    /**
     * Platform status & engine capability diagnostic.
     */
    @GetMapping("/trust/status")
    public ResponseEntity<Map<String, Object>> getEngineStatus() {
        boolean killActive = trustVerificationService.isKillSwitchActive();
        return ResponseEntity.ok(Map.of(
                "application", "TrAI Truth Infrastructure Platform",
                "version", "2.0.0",
                "status", killActive ? "HALTED_BY_KILL_SWITCH" : "OPERATIONAL",
                "killSwitchActive", killActive,
                "aiModels", List.of(
                        "xAI Grok-2-1212 (PRIMARY — pinned)",
                        "Google Vertex AI Gemini 2.0 Flash",
                        "OpenAI GPT-4o (ChatGPT filter)"
                ),
                "pipeline", "Dual-Agent Parallel (Agent1:Collector + Agent2:Validator)",
                "cloudProvider", "Google Cloud (Cloud Run + Vertex AI + GCS + Firebase FCM)",
                "features", List.of(
                        "Live Video/Speech Transcription Fact-Checking",
                        "AI Hallucination & Glitch Detection",
                        "Multi-Agent Parallel Validation (Grok+Gemini+ChatGPT)",
                        "Twitter/X Real Tweet Monitoring per Country",
                        "Country-Based Push Notifications (FCM — Android + iOS)",
                        "Propaganda & Double-Standard Stripping",
                        "Source Trust Scoring Index",
                        "B2B Webhook Gateway",
                        "Predictive Market Impact Analytics",
                        "SentinelMind Emergency Kill Switch"
                )
        ));
    }
}
