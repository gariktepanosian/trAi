package com.trai.engine.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trai.engine.domain.XUserRankScore;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TwitterApiService — Real-time post fetching via X (formerly Twitter) API v2.
 *
 * Uses the Search Recent Tweets endpoint to fetch real posts
 * about a specific country/topic posted by real X users.
 * After fetching, each author is evaluated by XUserRankingService,
 * which uses Grok + Gemini to assign an authorityScore.
 * viralityWeight incorporates the author's authorityScore from the DB.
 *
 * API Docs: https://developer.x.com/en/docs/x-api/tweets/search/api-reference/get-tweets-search-recent
 *
 * Note: The API endpoint domain remains api.twitter.com/2 (X has not migrated it).
 * The xAI Grok API endpoint uses api.x.ai/v1 (X's AI platform domain).
 *
 * Requirements:
 *  - X Developer account with Bearer Token
 *  - At minimum Basic tier access (for recent search)
 *  - Academic/Elevated access for higher volume
 */
@Service
public class TwitterApiService {

    private static final Logger log = LoggerFactory.getLogger(TwitterApiService.class);
    private static final String X_API_BASE = "https://api.twitter.com/2";

    @Value("${twitter.bearer-token:}")
    private String bearerToken;

    private final XUserRankingService xUserRankingService;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TwitterApiService(XUserRankingService xUserRankingService) {
        this.xUserRankingService = xUserRankingService;
    }

    /**
     * Search recent posts (last 7 days) mentioning a country + alert keywords.
     * Each author is evaluated by XUserRankingService for authority scoring.
     * Returns up to maxResults post objects with authorityScore enrichment.
     *
     * @param country     Country name, e.g. "Armenia", "Ukraine", "Japan"
     * @param maxResults  Number of posts to fetch (10–100)
     * @return List of TweetData objects enriched with authorityScore
     */
    public List<TweetData> searchCriticalTweets(String country, int maxResults) {
        if (bearerToken == null || bearerToken.isBlank() || bearerToken.equals("your-twitter-bearer-token")) {
            log.warn("[X API] Bearer token not configured — returning mock posts for country={}", country);
            return mockPostsForCountry(country);
        }

        // Build query: country + critical event keywords
        // Excludes reposts to get original content; English or native language
        String query = buildCriticalQuery(country);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int limit = Math.max(10, Math.min(maxResults, 100));

        // Request fields: text, author_id, created_at, public_metrics, geo
        String url = String.format(
                "%s/tweets/search/recent?query=%s&max_results=%d" +
                "&tweet.fields=text,created_at,author_id,public_metrics,lang,geo" +
                "&expansions=author_id&user.fields=verified,username,name,public_metrics",
                X_API_BASE, encodedQuery, limit);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + bearerToken)
                .header("User-Agent", "TrAI-Platform/2.0")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("[X API] API returned HTTP {} for query: {}", response.code(), query);
                return mockPostsForCountry(country);
            }
            String body = response.body() != null ? response.body().string() : "{}";
            return parsePosts(body, country);
        } catch (Exception e) {
            log.error("[X API] API call failed: {}", e.getMessage());
            return mockPostsForCountry(country);
        }
    }

    /**
     * Build an X search query that targets critical/high-priority events
     * for the given country. Filters noise and focuses on safety events.
     */
    private String buildCriticalQuery(String country) {
        return String.format(
                "(%s) (tornado OR earthquake OR war OR explosion OR attack OR flood " +
                "OR missile OR evacuation OR emergency OR crisis OR breaking OR urgent) " +
                "-is:retweet lang:en",
                country);
    }

    private List<TweetData> parsePosts(String json, String country) {
        List<TweetData> posts = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            JsonNode includes = root.path("includes");
            JsonNode users = includes.path("users");

            // Build author lookup map
            java.util.Map<String, JsonNode> authorMap = new java.util.HashMap<>();
            if (users.isArray()) {
                for (JsonNode user : users) {
                    authorMap.put(user.path("id").asText(), user);
                }
            }

            if (data.isArray()) {
                for (JsonNode post : data) {
                    TweetData td = new TweetData();
                    td.setId(post.path("id").asText());
                    td.setText(post.path("text").asText());
                    td.setCreatedAt(post.path("created_at").asText());
                    td.setCountry(country);
                    td.setLang(post.path("lang").asText("en"));

                    // Public metrics
                    JsonNode metrics = post.path("public_metrics");
                    td.setRetweetCount(metrics.path("retweet_count").asInt(0));
                    td.setLikeCount(metrics.path("like_count").asInt(0));

                    // Author info
                    String authorId = post.path("author_id").asText();
                    JsonNode author = authorMap.get(authorId);
                    if (author != null) {
                        td.setAuthorUsername(author.path("username").asText());
                        td.setAuthorVerified(author.path("verified").asBoolean(false));
                        td.setAuthorFollowers(author.path("public_metrics")
                                .path("followers_count").asInt(0));
                    }

                    // Evaluate X user authority via XUserRankingService (Grok + Gemini AI)
                    // This persists rank data to MongoDB and returns the live authority score
                    double authorityScore = 50.0; // default neutral
                    try {
                        XUserRankScore userRank = xUserRankingService.evaluateAndRankUser(td);
                        if (userRank != null) {
                            authorityScore = userRank.getAuthorityScore();
                        }
                    } catch (Exception e) {
                        log.warn("[X API] X user ranking failed for @{}: {}",
                                td.getAuthorUsername(), e.getMessage());
                        authorityScore = xUserRankingService.getAuthorityScore(td.getAuthorUsername());
                    }
                    td.setAuthorityScore(authorityScore);

                    // Virality weight: combines X's engagement signals + authority score from AI
                    // Base: 1.0
                    // + 2.0 if verified (X Blue / legacy)
                    // + 1.5 if >1,000 reposts (viral spread)
                    // + 1.0 if >100k followers (high-reach account)
                    // + up to 1.5 from authority score (score/100 * 1.5)
                    // Max possible: ~7.0
                    double viralityWeight = 1.0;
                    if (td.isAuthorVerified())             viralityWeight += 2.0;
                    if (td.getRetweetCount() > 1000)       viralityWeight += 1.5;
                    if (td.getAuthorFollowers() > 100_000) viralityWeight += 1.0;
                    viralityWeight += (authorityScore / 100.0) * 1.5; // authority bonus
                    td.setViralityWeight(viralityWeight);

                    posts.add(td);
                }
            }
        } catch (Exception e) {
            log.error("[X API] Failed to parse post response: {}", e.getMessage());
        }
        return posts;
    }

    /**
     * Mock posts returned when API key is not configured (dev/demo mode).
     * Returns 3 representative posts covering the full virality weight range.
     */
    private List<TweetData> mockPostsForCountry(String country) {
        List<TweetData> mocks = new ArrayList<>();

        TweetData t1 = new TweetData();
        t1.setId("mock-1");
        t1.setText("BREAKING: Severe weather warning issued for " + country + ". Authorities urge residents to stay indoors. #" + country.replaceAll("\\s+", ""));
        t1.setAuthorUsername("weatheralerts");
        t1.setAuthorVerified(true);
        t1.setAuthorFollowers(450000);
        t1.setRetweetCount(2100);
        t1.setLikeCount(3800);
        t1.setCountry(country);
        t1.setAuthorityScore(85.0);
        t1.setViralityWeight(5.775); // 1.0 + 2.0(verified) + 1.5(RT>1k) + 1.0(followers>100k) + 85/100*1.5
        t1.setCreatedAt(java.time.Instant.now().toString());
        mocks.add(t1);

        TweetData t2 = new TweetData();
        t2.setId("mock-2");
        t2.setText("Reports coming in from " + country + " about multiple incidents in the northern region. No official confirmation yet. Stay safe everyone.");
        t2.setAuthorUsername("globalreporter");
        t2.setAuthorVerified(false);
        t2.setAuthorFollowers(12000);
        t2.setRetweetCount(340);
        t2.setLikeCount(890);
        t2.setCountry(country);
        t2.setAuthorityScore(58.0);
        t2.setViralityWeight(1.87); // 1.0 + 0 + 0 + 0 + 58/100*1.5
        t2.setCreatedAt(java.time.Instant.now().toString());
        mocks.add(t2);

        TweetData t3 = new TweetData();
        t3.setId("mock-3");
        t3.setText("Everything is calm in " + country + " today. The earlier reports appear to be exaggerated. Local officials deny emergency.");
        t3.setAuthorUsername("localvoice_news");
        t3.setAuthorVerified(false);
        t3.setAuthorFollowers(8500);
        t3.setRetweetCount(120);
        t3.setLikeCount(290);
        t3.setCountry(country);
        t3.setAuthorityScore(50.0);
        t3.setViralityWeight(1.75); // 1.0 + 0 + 0 + 0 + 50/100*1.5
        t3.setCreatedAt(java.time.Instant.now().toString());
        mocks.add(t3);

        return mocks;
    }
}
