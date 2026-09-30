package com.trai.engine.scheduler;

import com.trai.engine.domain.SourceTrustScore;
import com.trai.engine.repository.SourceTrustScoreRepository;
import com.trai.engine.twitter.XUserRankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Daily trust score and authority rank recalculation job.
 *
 * Runs every 24 hours. Performs two recalculations:
 *
 *  1. Source Trust Scores (news/information sources):
 *     - Recomputes trustScore using real accuracy ratio from recorded AI verdicts
 *       (accurateArticleCount / totalArticlesChecked) minus accumulated propagandaPoints
 *     - Re-assigns integer credibilityRank (1 = most trusted)
 *     - Assigns credibilityTier based on final score
 *     - Sources with analyticsBootstrapped=false (seed data only) are lightly decayed
 *       to encourage re-validation once real data arrives
 *
 *  2. X User Authority Ranks (XUserRankingService):
 *     - Re-assigns integer authorityRank to all tracked X users
 *     - Reassigns tier labels based on current authorityScore
 *
 * This builds a compounding data moat — the longer TrAI runs,
 * the more accurate both the source trust index and X user authority index become.
 */
@Service
public class TrustScoreRecalculationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrustScoreRecalculationScheduler.class);

    private static final int BOOTSTRAP_THRESHOLD = 5;

    private final SourceTrustScoreRepository trustScoreRepository;
    private final XUserRankingService xUserRankingService;

    public TrustScoreRecalculationScheduler(
            SourceTrustScoreRepository trustScoreRepository,
            XUserRankingService xUserRankingService) {
        this.trustScoreRepository = trustScoreRepository;
        this.xUserRankingService = xUserRankingService;
    }

    /** Runs once every 24 hours (86,400,000 ms) */
    @Scheduled(fixedRate = 86_400_000)
    public void recalculateTrustScores() {
        log.info("[Scheduler] Starting daily trust score recalculation cycle...");
        recalculateSourceTrustScores();
        recalculateXUserRanks();
        log.info("[Scheduler] Daily recalculation cycle complete.");
    }

    // ─── Source Trust Score Recalculation ─────────────────────────────────────

    private void recalculateSourceTrustScores() {
        List<SourceTrustScore> sources = trustScoreRepository.findAll();
        if (sources.isEmpty()) {
            log.info("[Scheduler] No sources in trust registry yet. Skipping source recalculation.");
            return;
        }

        for (SourceTrustScore source : sources) {
            double recomputedScore = computeSourceScore(source);
            source.setTrustScore(recomputedScore);
            source.setCredibilityTier(computeSourceTier(recomputedScore));
        }

        // Re-rank all sources by descending trust score (1 = most trusted)
        sources.sort((a, b) -> Double.compare(b.getTrustScore(), a.getTrustScore()));
        for (int i = 0; i < sources.size(); i++) {
            sources.get(i).setCredibilityRank(i + 1);
        }

        trustScoreRepository.saveAll(sources);
        log.info("[Scheduler] Source trust score recalculation complete. {} sources updated.", sources.size());
    }

    /**
     * Compute trust score using real analytics data when available.
     * Falls back to mild decay for seed-only records.
     */
    private double computeSourceScore(SourceTrustScore source) {
        if (source.isAnalyticsBootstrapped()
                && source.getTotalArticlesChecked() >= BOOTSTRAP_THRESHOLD) {

            int total    = source.getTotalArticlesChecked();
            int accurate = source.getAccurateArticleCount();
            int misleading = source.getMisinformationArticleCount();

            // Accuracy ratio → base score
            double accuracyRatio = (double) accurate / total;
            double baseScore = accuracyRatio * 100.0;

            // Apply propaganda points penalty
            double penaltyScore = baseScore - source.getPropagandaPoints();

            // Extra boost for misinformation-free sources with high article count
            if (misleading == 0 && total >= 20) penaltyScore += 2.0;

            // Blend 60% new computation with 40% existing (avoid wild swings)
            double blended = (penaltyScore * 0.6) + (source.getTrustScore() * 0.4);
            return Math.max(0.0, Math.min(100.0, Math.round(blended * 10.0) / 10.0));

        } else {
            // Not enough real data yet — apply small decay to encourage re-check
            double decayed = source.getTrustScore() - source.getPropagandaPoints() * 0.1;
            return Math.max(0.0, Math.min(100.0, Math.round(decayed * 10.0) / 10.0));
        }
    }

    private String computeSourceTier(double score) {
        if (score >= 85) return "TIER_1";
        if (score >= 70) return "TIER_2";
        if (score >= 50) return "TIER_3";
        if (score >= 25) return "TIER_4";
        return "TIER_5";
    }

    // ─── X User Rank Recalculation ─────────────────────────────────────────────

    private void recalculateXUserRanks() {
        log.info("[Scheduler] Starting X user authority rank recalculation...");
        xUserRankingService.recalculateAllRanks();
    }
}
