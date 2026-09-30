package com.trai.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trai.engine.domain.SourceTrustScore;
import com.trai.engine.repository.SourceTrustScoreRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * SourceAuthorityService — AI-powered authority validation for news/information sources.
 *
 * Flow:
 *  1. A source URL arrives (from news normalization, manual input, or scraping).
 *  2. Grok checks the source for propaganda patterns and X/social media credibility signals.
 *  3. Gemini cross-validates against Google's knowledge graph.
 *  4. propagandaPoints are accumulated — each AI-detected issue adds a penalty.
 *  5. trustScore is adjusted: composite AI score minus propagandaPoints.
 *  6. credibilityTier (TIER_1 … TIER_5) is assigned.
 *  7. The result is persisted in MongoDB.
 *
 * Analytics bootstrapping:
 *  - On first run, the DB is seeded with well-known sources at known baseline scores.
 *  - Every news article processed adds to totalArticlesChecked and accuracy counts.
 *  - Once totalArticlesChecked >= 5, analyticsBootstrapped is true and data is live.
 *  - Even seeded records improve immediately when new articles come in.
 */
@Service
public class SourceAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(SourceAuthorityService.class);

    private static final int BOOTSTRAP_THRESHOLD = 5;

    private final SourceTrustScoreRepository trustScoreRepository;
    private final ChatLanguageModel grokModel;
    private final ChatLanguageModel geminiModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SourceAuthorityService(
            SourceTrustScoreRepository trustScoreRepository,
            @Qualifier("grokModel") ChatLanguageModel grokModel,
            @Qualifier("geminiModel") ChatLanguageModel geminiModel) {
        this.trustScoreRepository = trustScoreRepository;
        this.grokModel = grokModel;
        this.geminiModel = geminiModel;
    }

    /**
     * Validate a source's authority using Grok + Gemini.
     * Creates a new record if not seen before, updates if already tracked.
     * Returns null if URL is blank.
     */
    public SourceTrustScore validateAndRankSource(String sourceUrl, String sourceName) {
        if (sourceUrl == null || sourceUrl.isBlank()) return null;

        String normalizedUrl = normalizeUrl(sourceUrl);

        // Load existing or create new
        SourceTrustScore score = trustScoreRepository.findBySourceUrl(normalizedUrl)
                .orElseGet(() -> createNewSourceRecord(normalizedUrl, sourceName));

        // Run parallel AI authority checks
        String grokPrompt   = buildGrokSourcePrompt(normalizedUrl, sourceName);
        String geminiPrompt = buildGeminiSourcePrompt(normalizedUrl, sourceName);

        try {
            Future<String> grokFuture   = executor.submit(() -> safeAiCall(grokModel,   grokPrompt,   "Grok"));
            Future<String> geminiFuture = executor.submit(() -> safeAiCall(geminiModel, geminiPrompt, "Gemini"));

            String grokResponse   = grokFuture.get(25, TimeUnit.SECONDS);
            String geminiResponse = geminiFuture.get(25, TimeUnit.SECONDS);

            double grokScore      = extractDouble(grokResponse,   "authorityScore", 50.0);
            double geminiScore    = extractDouble(geminiResponse, "authorityScore", 50.0);
            double grokPenalty    = extractDouble(grokResponse,   "propagandaPenalty", 0.0);
            double geminiPenalty  = extractDouble(geminiResponse, "propagandaPenalty", 0.0);

            List<String> issues = mergeIssues(
                    extractStringList(grokResponse,   "detectedIssues"),
                    extractStringList(geminiResponse, "detectedIssues")
            );

            // Accumulate propaganda points (max 50)
            double totalPropaganda = score.getPropagandaPoints() + (grokPenalty * 0.6) + (geminiPenalty * 0.4);
            score.setPropagandaPoints(Math.min(totalPropaganda, 50.0));

            // Update detected issues (unique, capped at 20)
            if (!issues.isEmpty()) {
                Set<String> all = new LinkedHashSet<>();
                if (score.getDetectedIssues() != null) all.addAll(score.getDetectedIssues());
                all.addAll(issues);
                score.setDetectedIssues(all.stream().limit(20).toList());
            }

            // Compute composite trust score: weighted AI average minus propaganda penalty
            double compositeAi = (grokScore * 0.6) + (geminiScore * 0.4);
            double newScore = Math.max(0.0, Math.min(100.0, compositeAi - score.getPropagandaPoints()));

            // Blend with existing score (weighted running average — avoids single-event swings)
            if (score.isAnalyticsBootstrapped()) {
                double blended = (score.getTrustScore() * 0.7) + (newScore * 0.3);
                score.setTrustScore(Math.round(blended * 10.0) / 10.0);
            } else {
                score.setTrustScore(Math.round(newScore * 10.0) / 10.0);
            }

            score.setAiVerifiedBy("DUAL_GROK_GEMINI");
            score.setCredibilityTier(computeTier(score.getTrustScore()));
            score.setLastCheckedAt(Instant.now().toString());

            log.info("[SourceAuthority] {} trustScore={} tier={} propagandaPts={}",
                    normalizedUrl, score.getTrustScore(), score.getCredibilityTier(), score.getPropagandaPoints());

        } catch (Exception e) {
            log.warn("[SourceAuthority] AI check failed for {}: {}. Score unchanged.", normalizedUrl, e.getMessage());
        }

        score.setTotalArticlesChecked(score.getTotalArticlesChecked() + 1);
        if (score.getTotalArticlesChecked() >= BOOTSTRAP_THRESHOLD) {
            score.setAnalyticsBootstrapped(true);
        }

        return trustScoreRepository.save(score);
    }

    /**
     * Record an article's AI verdict for its source, updating accuracy/misinformation counts.
     * Called after MultiAgentValidationService finishes for a news article.
     *
     * @param sourceUrl  Source URL of the article
     * @param verdict    AI verdict: VERIFIED_TRUE, LIKELY_TRUE, MISLEADING, FALSE, UNVERIFIED_CLAIM
     * @param trustScore Article-level trust score (0–100)
     */
    public void recordArticleVerdict(String sourceUrl, String verdict, int trustScore) {
        if (sourceUrl == null || sourceUrl.isBlank()) return;
        String normalizedUrl = normalizeUrl(sourceUrl);

        trustScoreRepository.findBySourceUrl(normalizedUrl).ifPresent(score -> {
            score.setTotalArticlesChecked(score.getTotalArticlesChecked() + 1);

            boolean isMisinformation = verdict.equals("FALSE") || verdict.equals("MISLEADING");
            boolean isAccurate       = verdict.equals("VERIFIED_TRUE") || verdict.equals("LIKELY_TRUE");

            if (isMisinformation) {
                score.setMisinformationArticleCount(score.getMisinformationArticleCount() + 1);
                // Each confirmed misinformation article adds a small propaganda penalty
                double penalty = verdict.equals("FALSE") ? 2.0 : 1.0;
                score.setPropagandaPoints(Math.min(score.getPropagandaPoints() + penalty, 50.0));
            }
            if (isAccurate) {
                score.setAccurateArticleCount(score.getAccurateArticleCount() + 1);
            }

            // After enough data, recompute trust score from accuracy ratio
            if (score.getTotalArticlesChecked() >= BOOTSTRAP_THRESHOLD) {
                score.setAnalyticsBootstrapped(true);
                double total  = score.getTotalArticlesChecked();
                double accurate = score.getAccurateArticleCount();
                double accuracyRatio = total > 0 ? (accurate / total) : 0.5;
                // Blend: 70% accuracy ratio → score, 30% current score, minus propaganda
                double computedScore = (accuracyRatio * 100 * 0.7)
                        + (score.getTrustScore() * 0.3)
                        - score.getPropagandaPoints();
                score.setTrustScore(Math.max(0.0, Math.min(100.0, Math.round(computedScore * 10.0) / 10.0)));
                score.setCredibilityTier(computeTier(score.getTrustScore()));
            }

            trustScoreRepository.save(score);
        });
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private SourceTrustScore createNewSourceRecord(String sourceUrl, String sourceName) {
        SourceTrustScore s = new SourceTrustScore();
        s.setId(sourceUrl.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(50, sourceUrl.length())));
        s.setSourceUrl(sourceUrl);
        s.setSourceName(sourceName != null && !sourceName.isBlank() ? sourceName : sourceUrl);
        s.setTrustScore(50.0); // neutral until AI validates
        s.setCredibilityTier("TIER_3");
        s.setPropagandaPoints(0.0);
        s.setTotalArticlesChecked(0);
        s.setAccurateArticleCount(0);
        s.setMisinformationArticleCount(0);
        s.setAnalyticsBootstrapped(false);
        s.setDetectedIssues(new ArrayList<>());
        s.setFirstSeenAt(Instant.now().toString());
        s.setFirstSeenAt(Instant.now().toString());
        s.setLastCheckedAt(Instant.now().toString());
        log.info("[SourceAuthority] New source encountered: {}", sourceUrl);
        return s;
    }

    private String buildGrokSourcePrompt(String sourceUrl, String sourceName) {
        return String.format("""
            You are Grok AI evaluating the authority and credibility of a news/information source.
            
            SOURCE URL: %s
            SOURCE NAME: %s
            
            Task: Assess this source's authority, credibility, and potential for propaganda or misinformation.
            
            Consider:
            1. Is this a known credible news outlet, government source, or research institution?
            2. Does this source have a known history of propaganda, bias, or misinformation?
            3. Is this a primary source, secondary, or aggregator?
            4. Are there known fact-checking strikes against this source?
            
            propagandaPenalty: 0–50 (0=clean reputable source, 50=confirmed propaganda outlet)
            
            Respond ONLY in this JSON format (no markdown):
            {
              "model": "GROK",
              "sourceUrl": "%s",
              "authorityScore": <0-100>,
              "propagandaPenalty": <0-50>,
              "sourceCategory": "TIER_1_INSTITUTIONAL | TIER_2_REPUTABLE | TIER_3_MIXED | TIER_4_BIASED | TIER_5_UNRELIABLE",
              "detectedIssues": ["PROPAGANDA" | "SELECTIVE_REPORTING" | "EMOTIONAL_LANGUAGE" | "UNVERIFIABLE_CLAIMS" | "KNOWN_MISINFORMATION_SOURCE"],
              "authoritySignals": "<key credibility signals observed>",
              "summary": "<2-sentence authority assessment>"
            }
            """, sourceUrl, sourceName != null ? sourceName : sourceUrl, sourceUrl);
    }

    private String buildGeminiSourcePrompt(String sourceUrl, String sourceName) {
        return String.format("""
            You are Gemini AI cross-validating the authority of a news/information source.
            
            SOURCE URL: %s
            SOURCE NAME: %s
            
            Task: Use your Google knowledge graph to assess this source's credibility.
            
            Consider:
            1. Is this source indexed and trusted by Google News?
            2. Does Google's knowledge graph recognize this as a credible outlet?
            3. Are there signals of this source spreading misinformation?
            
            propagandaPenalty: 0–50 (0=clean, 50=confirmed propaganda/misinformation outlet)
            
            Respond ONLY in this JSON format (no markdown):
            {
              "model": "GEMINI",
              "sourceUrl": "%s",
              "authorityScore": <0-100>,
              "propagandaPenalty": <0-50>,
              "detectedIssues": ["PROPAGANDA" | "SELECTIVE_REPORTING" | "KNOWN_MISINFORMATION_SOURCE" | "NOT_IN_KNOWLEDGE_GRAPH"],
              "knowledgeGraphSignals": "<what Google knowledge signals indicate about this source>",
              "summary": "<2-sentence cross-validation result>"
            }
            """, sourceUrl, sourceName != null ? sourceName : sourceUrl, sourceUrl);
    }

    private String safeAiCall(ChatLanguageModel model, String prompt, String modelName) {
        try {
            return model.generate(prompt);
        } catch (Exception e) {
            log.warn("[SourceAuthority] {} AI call failed: {}", modelName, e.getMessage());
            return "{\"authorityScore\":50,\"propagandaPenalty\":0,\"detectedIssues\":[]}";
        }
    }

    private double extractDouble(String json, String fieldName, double defaultValue) {
        if (json == null) return defaultValue;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has(fieldName)) return node.get(fieldName).asDouble(defaultValue);
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private List<String> extractStringList(String json, String fieldName) {
        if (json == null) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode arr = node.get(fieldName);
            if (arr != null && arr.isArray()) {
                List<String> result = new ArrayList<>();
                arr.forEach(el -> result.add(el.asText()));
                return result;
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private List<String> mergeIssues(List<String> a, List<String> b) {
        Set<String> merged = new LinkedHashSet<>();
        if (a != null) merged.addAll(a);
        if (b != null) merged.addAll(b);
        return new ArrayList<>(merged);
    }

    private String computeTier(double score) {
        if (score >= 85) return "TIER_1";
        if (score >= 70) return "TIER_2";
        if (score >= 50) return "TIER_3";
        if (score >= 25) return "TIER_4";
        return "TIER_5";
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.trim().toLowerCase()
                .replaceAll("^https?://", "")
                .replaceAll("^www\\.", "")
                .replaceAll("/$", "");
    }
}
