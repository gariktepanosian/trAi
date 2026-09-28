package com.trai.engine.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NewsSearchService Unit Tests")
class NewsSearchServiceTest {

    private final NewsSearchService searchService = new NewsSearchService();

    @Test
    @DisplayName("Should return matches for keyword search")
    void testSearchMatches() {
        List<NewsSearchDocument> results = searchService.searchNews("interest rate");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).title().contains("Interest Rate"));
    }

    @Test
    @DisplayName("Should index new document and find it")
    void testIndexAndSearch() {
        NewsSearchDocument doc = new NewsSearchDocument(
                "doc-99",
                "Breakthrough Energy Technology Announced",
                "New fusion reactor test achieves net energy gain.",
                "TechWire",
                List.of("Net energy gain achieved"),
                List.of(),
                96.0,
                Instant.now()
        );
        searchService.indexNews(doc);

        List<NewsSearchDocument> results = searchService.searchNews("fusion");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("doc-99", results.get(0).id());
    }
}
