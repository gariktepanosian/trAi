package com.trai.engine.scheduler;

import com.trai.engine.ai.AntiPropagandaEngine;
import com.trai.engine.repository.NormalizedNewsRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class NewsScrapingScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapingScheduler.class);
    private final AntiPropagandaEngine aiEngine;
    private final NormalizedNewsRepository repository;

    public NewsScrapingScheduler(AntiPropagandaEngine aiEngine, NormalizedNewsRepository repository) {
        this.aiEngine = aiEngine;
        this.repository = repository;
    }

    // Runs every 10 minutes (600,000 ms)
    @Scheduled(fixedRate = 600000)
    public void scrapeAndNormalizePipeline() {
        log.info("Starting 10-minute TrAI scraping cycle...");

        try {
            // 1. Scrape raw data (Example: a specific target site)
            Document doc = Jsoup.connect("https://example.com/news").get();
            String rawText = doc.body().text();

            // 2. Pass to AI for Propaganda Stripping & JSON conversion
            log.info("Passing raw data to Grok/Gemini for normalization...");
            String cleanJsonData = aiEngine.normalizeNewsData(rawText);

            log.info("Parsed output format: {}", cleanJsonData);

            // 3. TODO: Map cleanJsonData JSON → NormalizedNews and save to MongoDB
            // repository.save(...);
            log.info("Normalized data processing complete.");

        } catch (IOException e) {
            log.error("Scraping failed during cycle: ", e);
        }
    }
}
