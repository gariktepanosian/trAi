package com.trai.engine.search;

import java.time.Instant;
import java.util.List;

/**
 * Search document representation of indexed news stories for Elasticsearch / Atlas Full-Text.
 */
public record NewsSearchDocument(
        String id,
        String title,
        String summary,
        String sourceName,
        List<String> keyFacts,
        List<String> propagandaFlags,
        double trustScore,
        Instant indexedAt
) {}
