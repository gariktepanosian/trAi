package com.trai.engine.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * High-performance full-text search indexing service for scraped and normalized news.
 * Provides indexing and fuzzy full-text query capabilities.
 */
@Service
public class NewsSearchService {

    private static final Logger log = LoggerFactory.getLogger(NewsSearchService.class);

    private final ConcurrentMap<String, NewsSearchDocument> indexStore = new ConcurrentHashMap<>();

    public NewsSearchService() {
        seedInitialIndex();
    }

    public void indexNews(NewsSearchDocument doc) {
        log.info("Indexing news story into search vault: [{}] {}", doc.id(), doc.title());
        indexStore.put(doc.id(), doc);
    }

    @Cacheable(value = "news-search", key = "#query")
    public List<NewsSearchDocument> searchNews(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(indexStore.values());
        }

        String lower = query.toLowerCase();
        return indexStore.values().stream()
                .filter(doc -> doc.title().toLowerCase().contains(lower) ||
                               doc.summary().toLowerCase().contains(lower) ||
                               doc.sourceName().toLowerCase().contains(lower) ||
                               doc.keyFacts().stream().anyMatch(f -> f.toLowerCase().contains(lower)))
                .toList();
    }

    private void seedInitialIndex() {
        indexNews(new NewsSearchDocument(
                "idx-1",
                "Central Bank Stabilises Benchmark Interest Rate",
                "Central bank governors voted unanimously to hold the policy rate following core inflation deceleration.",
                "Reuters",
                List.of("Policy rate maintained at 4.25%", "Core inflation slowed to 2.8%"),
                List.of(),
                94.5,
                Instant.now()
        ));

        indexNews(new NewsSearchDocument(
                "idx-2",
                "International Maritime Transit Agreement Ratified",
                "Maritime safety corridor treaty ratified by 28 coastal nations in Singapore.",
                "Associated Press",
                List.of("28 signatory states ratified treaty", "Commercial insurance premiums expected to drop 15%"),
                List.of(),
                93.8,
                Instant.now()
        ));
    }
}
