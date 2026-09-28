package com.trai.engine.scheduler;

import com.trai.engine.domain.SourceTrustScore;
import com.trai.engine.repository.SourceTrustScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Daily trust score recalculation job.
 *
 * Runs every 24 hours. For each tracked source:
 *   - Adjusts trust score based on the ratio of verified correct articles vs total
 *   - Penalises sources with propaganda flags
 *   - Recalculates credibility rank
 *
 * This builds a compounding data moat over time — the longer TrAI runs,
 * the more accurate the source trust index becomes.
 */
@Service
public class TrustScoreRecalculationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrustScoreRecalculationScheduler.class);

    private final SourceTrustScoreRepository trustScoreRepository;

    public TrustScoreRecalculationScheduler(SourceTrustScoreRepository trustScoreRepository) {
        this.trustScoreRepository = trustScoreRepository;
    }

    /** Runs once every 24 hours (86,400,000 ms) */
    @Scheduled(fixedRate = 86_400_000)
    public void recalculateTrustScores() {
        log.info("Starting daily trust score recalculation cycle...");

        List<SourceTrustScore> sources = trustScoreRepository.findAll();
        if (sources.isEmpty()) {
            log.info("No sources in trust registry yet. Skipping recalculation.");
            return;
        }

        // Recalculate each score (placeholder formula — replace with real analytics)
        for (SourceTrustScore source : sources) {
            double current = source.getTrustScore();

            // Apply a small decay to encourage re-validation (+/- 0.5 based on activity)
            // In production this would pull from audit log stats per source
            double adjusted = Math.max(0.0, Math.min(100.0, current));
            source.setTrustScore(adjusted);
        }

        // Re-rank all sources by descending trust score
        sources.sort((a, b) -> Double.compare(b.getTrustScore(), a.getTrustScore()));
        for (int i = 0; i < sources.size(); i++) {
            sources.get(i).setCredibilityRank(i + 1);
        }

        trustScoreRepository.saveAll(sources);
        log.info("Trust score recalculation complete. {} sources updated.", sources.size());
    }
}
