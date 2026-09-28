package com.trai.engine.analytics;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AI interface for predictive market impact analysis.
 * Predicts short-term movement of gold, BTC, oil, and USD based on geopolitical events.
 */
public interface MarketImpactEngine {

    @SystemMessage("""
            You are TrAI's quantitative geopolitical analyst.
            You analyze verified news events for their probable short-term market impact.
            You have no political opinions. You identify historical patterns only.

            For each asset class, you reason from:
            - Historical precedents of similar events
            - Current market sentiment signals
            - Supply/demand shock potential
            - Safe-haven vs risk-on dynamics

            Always return ONLY valid JSON — no prose before or after.
            """)
    @UserMessage("""
            Analyze this verified event summary for market impact (next 24-72 hours):
            
            EVENT: {{eventSummary}}
            
            Return JSON:
            {
              "gold":    { "direction": "UP|DOWN|NEUTRAL", "magnitude": "LOW|MEDIUM|HIGH", "confidence": 0.0 },
              "btc":     { "direction": "UP|DOWN|NEUTRAL", "magnitude": "LOW|MEDIUM|HIGH", "confidence": 0.0 },
              "oil":     { "direction": "UP|DOWN|NEUTRAL", "magnitude": "LOW|MEDIUM|HIGH", "confidence": 0.0 },
              "usd":     { "direction": "UP|DOWN|NEUTRAL", "magnitude": "LOW|MEDIUM|HIGH", "confidence": 0.0 },
              "keyFactors": ["factor1", "factor2"],
              "timeframe": "24h|48h|72h",
              "caveat": "What could invalidate this prediction"
            }
            """)
    String predictMarketImpact(@V("eventSummary") String eventSummary);
}
