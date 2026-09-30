package com.trai.engine.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trai.engine.ai.AntiPropagandaEngine;
import com.trai.engine.domain.NormalizedNews;
import com.trai.engine.repository.NormalizedNewsRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * NewsScrapingScheduler — scrapes real news sources every 10 minutes,
 * normalizes content through the Anti-Propaganda AI engine, and persists to MongoDB.
 *
 * Sources: Reuters, AP News, BBC World
 * All scraped text is passed through Grok (AntiPropagandaEngine) for:
 *  - Propaganda stripping
 *  - Neutral reformatting
 *  - JSON structured output with trust score
 */
@Service
public class NewsScrapingScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapingScheduler.class);

    private static final List<String[]> NEWS_SOURCES = List.of(
            new String[]{"Reuters World", "https://feeds.reuters.com/reuters/worldNews"},
            new String[]{"AP News", "https://apnews.com/hub/world-news"},
            new String[]{"BBC World", "https://www.bbc.com/news/world"}
    );

    private final AntiPropagandaEngine aiEngine;
    private final NormalizedNewsRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsScrapingScheduler(AntiPropagandaEngine aiEngine,
                                  NormalizedNewsRepository repository) {
        this.aiEngine = aiEngine;
        this.repository = repository;
    }

    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    public void scrapeAndNormalizePipeline() {
        log.info("[Scraper] Starting 10-minute news scraping cycle...");

        for (String[] source : NEWS_SOURCES) {
            String sourceName = source[0];
            String url = source[1];
            try {
                scrapeSource(sourceName, url);
            } catch (Exception e) {
                log.warn("[Scraper] Failed to scrape {}: {}", sourceName, e.getMessage());
            }
        }

        log.info("[Scraper] Scraping cycle complete.");
    }

    private void scrapeSource(String sourceName, String url) throws Exception {
        log.info("[Scraper] Scraping source: {} ({})", sourceName, url);

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; TrAI-Bot/2.0)")
                .timeout(10_000)
                .get();

        // Extract headlines and article summaries
        List<String> articleTexts = extractArticleTexts(doc, sourceName);

        if (articleTexts.isEmpty()) {
            log.warn("[Scraper] No articles extracted from {}", sourceName);
            return;
        }

        // Process up to 5 articles per source per cycle to avoid rate limits
        int processed = 0;
        for (String articleText : articleTexts) {
            if (processed >= 5) break;
            if (articleText.length() < 50) continue;

            try {
                processArticle(sourceName, url, articleText);
                processed++;
                Thread.sleep(1000); // Be polite to sources
            } catch (Exception e) {
                log.warn("[Scraper] Failed to process article from {}: {}", sourceName, e.getMessage());
            }
        }

        log.info("[Scraper] Processed {} articles from {}", processed, sourceName);
    }

    private List<String> extractArticleTexts(Document doc, String sourceName) {
        List<String> texts = new ArrayList<>();

        // Try multiple selectors for different news site layouts
        String[][] selectorSets = {
                {"h2 a", "h3 a", "h1 a"},             // Reuters, AP
                {".story-body p", ".article__body p"},  // BBC
                {"article p", ".entry-content p"},      // Generic
        };

        for (String[] selectors : selectorSets) {
            for (String selector : selectors) {
                Elements elements = doc.select(selector);
                for (Element el : elements) {
                    String text = el.text().trim();
                    if (text.length() > 50) {
                        texts.add(text);
                    }
                }
                if (!texts.isEmpty()) break;
            }
            if (!texts.isEmpty()) break;
        }

        // Fallback: grab all paragraph text from body
        if (texts.isEmpty()) {
            Elements paragraphs = doc.select("p");
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (text.length() > 80) texts.add(text);
            }
        }

        return texts;
    }

    private void processArticle(String sourceName, String sourceUrl, String rawText) {
        // Pass to AI for propaganda stripping and JSON normalization
        String cleanJson;
        try {
            cleanJson = aiEngine.normalizeNewsData(rawText);
        } catch (Exception e) {
            log.warn("[Scraper] AI normalization failed for article from {}: {}", sourceName, e.getMessage());
            // Save with raw text as fallback
            cleanJson = buildFallbackJson(rawText, sourceName);
        }

        // Map JSON output → NormalizedNews domain object and save to MongoDB
        NormalizedNews news = mapToNormalizedNews(cleanJson, sourceName, sourceUrl, rawText);
        try {
            repository.save(news);
            log.debug("[Scraper] Saved normalized article from {} | trustScore={}",
                    sourceName, news.getTrustScore());
        } catch (Exception e) {
            log.error("[Scraper] Failed to save article to MongoDB: {}", e.getMessage());
        }
    }

    private NormalizedNews mapToNormalizedNews(String json, String sourceName,
                                                String sourceUrl, String rawText) {
        NormalizedNews news = new NormalizedNews();
        news.setSourceName(sourceName);
        news.setSourceUrl(sourceUrl);
        news.setRawText(rawText.substring(0, Math.min(rawText.length(), 500)));
        news.setCreatedAt(Instant.now());

        try {
            JsonNode node = objectMapper.readTree(json);
            news.setNormalizedText(node.path("normalizedSummary")
                    .asText(node.path("summary").asText(rawText)));
            news.setTitle(node.path("headline").asText(node.path("title").asText("")));
            news.setTrustScore(node.path("trustScore")
                    .asDouble(node.path("confidence").asDouble(70.0)));
            news.setPropagandaScore(node.path("propagandaScore")
                    .asDouble(node.path("biasScore").asDouble(20.0)));
        } catch (Exception e) {
            // JSON parse failed — store raw text
            news.setNormalizedText(rawText.substring(0, Math.min(rawText.length(), 300)));
            news.setTrustScore(70.0);
            news.setPropagandaScore(20.0);
        }

        return news;
    }

    private String buildFallbackJson(String rawText, String sourceName) {
        return String.format(
                "{\"normalizedSummary\":\"%s\",\"trustScore\":65,\"propagandaScore\":25,\"source\":\"%s\"}",
                rawText.replace("\"", "'").substring(0, Math.min(rawText.length(), 200)),
                sourceName);
    }
}
