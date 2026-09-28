package com.trai.engine.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Predictive analytics service.
 *
 * Takes a verified event summary (from the AntiPropagandaEngine output)
 * and predicts short-term market impact using the MarketImpactEngine AI interface.
 *
 * Results are cached for 15 minutes to avoid redundant API calls for the same event.
 */
@Service
public class PredictiveAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalyticsService.class);

    private final MarketImpactEngine marketImpactEngine;

    public PredictiveAnalyticsService(MarketImpactEngine marketImpactEngine) {
        this.marketImpactEngine = marketImpactEngine;
    }

    /**
     * Predict market impact for a given event summary.
     * Cache key = first 100 chars of eventSummary (good enough for deduplication).
     */
    @Cacheable(value = "analytics-results", key = "#eventSummary.substring(0, T(Math).min(#eventSummary.length(), 100))")
    public Map<String, Object> predictImpact(String eventSummary) {
        if (eventSummary == null || eventSummary.isBlank()) {
            return Map.of("error", "Event summary is required.");
        }

        try {
            log.info("Running market impact prediction for event: {}...",
                    eventSummary.substring(0, Math.min(60, eventSummary.length())));

            String rawJson = marketImpactEngine.predictMarketImpact(eventSummary);

            return Map.of(
                    "status", "SUCCESS",
                    "mode", "AI_PREDICTION",
                    "eventSummary", eventSummary.substring(0, Math.min(200, eventSummary.length())),
                    "prediction", rawJson,
                    "predictedAt", Instant.now().toString()
            );
        } catch (Exception e) {
            log.warn("Market impact prediction failed: {}. Using heuristic fallback.", e.getMessage());
            return fallbackPrediction(eventSummary);
        }
    }

    /** Fallback prediction when AI is unavailable */
    private Map<String, Object> fallbackPrediction(String eventSummary) {
        String lower = eventSummary.toLowerCase();
        String goldDir = "NEUTRAL";
        String btcDir  = "NEUTRAL";
        String oilDir  = "NEUTRAL";

        if (lower.contains("war") || lower.contains("sanction") || lower.contains("conflict")) {
            goldDir = "UP";
            oilDir  = "UP";
            btcDir  = "UP"; // Flight to digital safe haven
        } else if (lower.contains("ceasefire") || lower.contains("peace") || lower.contains("agreement")) {
            goldDir = "DOWN";
            oilDir  = "DOWN";
        } else if (lower.contains("rate hike") || lower.contains("fed") || lower.contains("inflation")) {
            goldDir = "DOWN";
            btcDir  = "DOWN";
        }

        return Map.of(
                "status", "SUCCESS",
                "mode", "HEURISTIC_FALLBACK",
                "prediction", Map.of(
                        "gold",  Map.of("direction", goldDir,  "magnitude", "LOW", "confidence", 0.45),
                        "btc",   Map.of("direction", btcDir,   "magnitude", "LOW", "confidence", 0.40),
                        "oil",   Map.of("direction", oilDir,   "magnitude", "LOW", "confidence", 0.50),
                        "usd",   Map.of("direction", "NEUTRAL","magnitude", "LOW", "confidence", 0.30),
                        "caveat", "Heuristic model — real AI prediction unavailable"
                ),
                "predictedAt", Instant.now().toString()
        );
    }
}
