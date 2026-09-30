package com.trai.engine.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trai.engine.domain.XUserRankScore;
import com.trai.engine.repository.XUserRankScoreRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * XUserRankingService — AI-powered authority ranking system for X (formerly Twitter) users.
 *
 * How it works:
 *  1. When TrAI processes a tweet, this service is called with the author's metadata.
 *  2. If the user is not yet in the DB, a new XUserRankScore is created.
 *  3. Grok AI analyzes the user's credibility signals (verified, followers, engagement).
 *  4. Gemini AI cross-validates by checking Google knowledge graph signals.
 *  5. propagandaPoints are accumulated from AI-detected issues (propaganda, misinformation).
 *  6. authorityScore is computed as a composite of AI scores minus propagandaPoints penalty.
 *  7. authorityTier (TIER_1 … TIER_5) is assigned.
 *  8. The result is persisted in MongoDB and returned — now usable for virality weighting.
 *
 * Analytics bootstrapping:
 *  - The DB starts empty. Every tweet processed adds data.
 *  - Once totalPostsAnalyzed >= 3 for a user, analyticsBootstrapped is set to true.
 *  - The daily scheduler recalculates all ranks once enough data exists.
 *  - Even on first encounter, an initial AI assessment produces a meaningful score.
 *
 * Authority Tier scale:
 *  TIER_1 (85–100): Government, major verified journalists, major outlets on X
 *  TIER_2 (70–84):  Consistent, reputable posters
 *  TIER_3 (50–69):  Mixed record — use with caution
 *  TIER_4 (25–49):  High bias or detected misinformation
 *  TIER_5 (0–24):   Unreliable — consistent propaganda
 */
@Service
public class XUserRankingService {

    private static final Logger log = LoggerFactory.getLogger(XUserRankingService.class);

    private static final int BOOTSTRAP_THRESHOLD = 3; // posts needed before marking bootstrapped

    private final XUserRankScoreRepository xUserRankScoreRepository;
    private final ChatLanguageModel grokModel;
    private final ChatLanguageModel geminiModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public XUserRankingService(
            XUserRankScoreRepository xUserRankScoreRepository,
            @Qualifier("grokModel") ChatLanguageModel grokModel,
            @Qualifier("geminiModel") ChatLanguageModel geminiModel) {
        this.xUserRankScoreRepository = xUserRankScoreRepository;
        this.grokModel = grokModel;
        this.geminiModel = geminiModel;
    }

    /**
     * Evaluate and rank an X user based on their tweet metadata.
     * Creates a new record if not seen before, updates if already tracked.
     *
     * @param tweet  The tweet whose author we want to rank
     * @return       Updated XUserRankScore for the author
     */
    public XUserRankScore evaluateAndRankUser(TweetData tweet) {
        if (tweet == null || tweet.getAuthorUsername() == null || tweet.getAuthorUsername().isBlank()) {
            return null;
        }

        String username = tweet.getAuthorUsername().toLowerCase().trim();

        // Load or create user rank record
        XUserRankScore rankScore = xUserRankScoreRepository.findByUsername(username)
                .orElseGet(() -> createNewUserRecord(tweet));

        // Update raw metadata from latest tweet data
        rankScore.setVerified(tweet.isAuthorVerified());
        if (tweet.getAuthorFollowers() > 0) {
            rankScore.setFollowerCount(tweet.getAuthorFollowers());
        }
        rankScore.setTotalPostsAnalyzed(rankScore.getTotalPostsAnalyzed() + 1);

        // Run Grok + Gemini AI assessments in parallel
        try {
            String grokPrompt  = buildGrokAuthorityPrompt(tweet);
            String geminiPrompt = buildGeminiAuthorityPrompt(tweet);

            Future<String> grokFuture   = executor.submit(() -> safeAiCall(grokModel,   grokPrompt,   "Grok"));
            Future<String> geminiFuture = executor.submit(() -> safeAiCall(geminiModel, geminiPrompt, "Gemini"));

            String grokResponse   = grokFuture.get(25, TimeUnit.SECONDS);
            String geminiResponse = geminiFuture.get(25, TimeUnit.SECONDS);

            // Parse AI responses
            double grokScore   = extractScore(grokResponse,   "authorityScore", 50.0);
            double geminiScore = extractScore(geminiResponse, "authorityScore", 50.0);
            double propagandaPenalty = extractScore(grokResponse, "propagandaPenalty", 0.0)
                                     + extractScore(geminiResponse, "propagandaPenalty", 0.0);
            List<String> issues = mergeIssues(
                    extractStringList(grokResponse,   "detectedIssues"),
                    extractStringList(geminiResponse, "detectedIssues")
            );

            // Update running AI totals
            rankScore.setGrokScore(grokScore);
            rankScore.setGeminiScore(geminiScore);
            rankScore.setGrokAssessment(grokResponse);
            rankScore.setGeminiAssessment(geminiResponse);

            // Accumulate propaganda points (persists across evaluations)
            double totalPropagandaPoints = rankScore.getPropagandaPoints() + propagandaPenalty;
            rankScore.setPropagandaPoints(Math.min(totalPropagandaPoints, 50.0)); // cap at 50

            // Track misinformation and accurate post counts
            boolean isMisinformation = issues.stream()
                    .anyMatch(i -> i.contains("MISINFORMATION") || i.contains("FALSE") || i.contains("PROPAGANDA"));
            boolean isAccurate = issues.isEmpty() && (grokScore >= 70 && geminiScore >= 70);

            if (isMisinformation) rankScore.setMisinformationCount(rankScore.getMisinformationCount() + 1);
            if (isAccurate)       rankScore.setVerifiedAccurateCount(rankScore.getVerifiedAccurateCount() + 1);

            if (!issues.isEmpty()) {
                List<String> all = new ArrayList<>(rankScore.getDetectedIssues() != null
                        ? rankScore.getDetectedIssues() : List.of());
                all.addAll(issues);
                // Keep unique, cap at 20 most recent
                Set<String> unique = new LinkedHashSet<>(all);
                List<String> capped = unique.stream().limit(20).toList();
                rankScore.setDetectedIssues(capped);
            }

            // Compute composite authority score
            double compositeAiScore = (grokScore * 0.6) + (geminiScore * 0.4);
            double baseScore = compositeAiScore - rankScore.getPropagandaPoints();
            baseScore = Math.max(0.0, Math.min(100.0, baseScore));
            rankScore.setAuthorityScore(baseScore);

        } catch (Exception e) {
            log.warn("[XUserRanking] AI assessment failed for @{}: {}. Using heuristic score.", username, e.getMessage());
            // Fallback: heuristic authority score from metadata alone
            double heuristicScore = computeHeuristicScore(tweet);
            rankScore.setAuthorityScore(Math.max(rankScore.getAuthorityScore(), heuristicScore));
        }

        // Assign tier
        rankScore.setAuthorityTier(computeTier(rankScore.getAuthorityScore()));

        // Mark bootstrapped once enough posts have been analyzed
        if (rankScore.getTotalPostsAnalyzed() >= BOOTSTRAP_THRESHOLD) {
            rankScore.setAnalyticsBootstrapped(true);
        }

        rankScore.setLastCheckedAt(Instant.now().toString());

        // Persist and return
        XUserRankScore saved = xUserRankScoreRepository.save(rankScore);
        log.info("[XUserRanking] @{} authorityScore={} tier={} propagandaPts={} postsAnalyzed={}",
                username, saved.getAuthorityScore(), saved.getAuthorityTier(),
                saved.getPropagandaPoints(), saved.getTotalPostsAnalyzed());

        return saved;
    }

    /**
     * Recalculate and reassign integer authorityRank for all tracked X users.
     * Called by the daily scheduler.
     */
    public void recalculateAllRanks() {
        List<XUserRankScore> allUsers = xUserRankScoreRepository.findAllByOrderByAuthorityScoreDesc();
        if (allUsers.isEmpty()) {
            log.info("[XUserRanking] No X users in rank registry yet. Skipping recalculation.");
            return;
        }
        for (int i = 0; i < allUsers.size(); i++) {
            XUserRankScore user = allUsers.get(i);
            user.setAuthorityRank(i + 1);
            user.setAuthorityTier(computeTier(user.getAuthorityScore()));
        }
        xUserRankScoreRepository.saveAll(allUsers);
        log.info("[XUserRanking] Rank recalculation complete. {} X users re-ranked.", allUsers.size());
    }

    /**
     * Get the authority score for a username from the DB, or 50.0 (neutral) if unknown.
     */
    public double getAuthorityScore(String username) {
        if (username == null || username.isBlank()) return 50.0;
        return xUserRankScoreRepository.findByUsername(username.toLowerCase().trim())
                .map(XUserRankScore::getAuthorityScore)
                .orElse(50.0);
    }

    /**
     * Get all ranked X users (sorted best first).
     */
    public List<XUserRankScore> getAllRankedUsers() {
        return xUserRankScoreRepository.findAllByOrderByAuthorityScoreDesc();
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private XUserRankScore createNewUserRecord(TweetData tweet) {
        XUserRankScore r = new XUserRankScore();
        r.setId(tweet.getAuthorUsername().toLowerCase().trim());
        r.setUsername(tweet.getAuthorUsername().toLowerCase().trim());
        r.setVerified(tweet.isAuthorVerified());
        r.setFollowerCount(tweet.getAuthorFollowers());
        r.setAuthorityScore(computeHeuristicScore(tweet)); // start with heuristic
        r.setAuthorityTier(computeTier(r.getAuthorityScore()));
        r.setPropagandaPoints(0.0);
        r.setMisinformationCount(0);
        r.setVerifiedAccurateCount(0);
        r.setTotalPostsAnalyzed(0);
        r.setAnalyticsBootstrapped(false);
        r.setDetectedIssues(new ArrayList<>());
        r.setFirstSeenAt(Instant.now().toString());
        r.setLastCheckedAt(Instant.now().toString());
        log.info("[XUserRanking] New X user encountered: @{} (verified={} followers={})",
                r.getUsername(), r.isVerified(), r.getFollowerCount());
        return r;
    }

    /**
     * Heuristic authority score from metadata alone (used before AI assessment
     * and as fallback when AI is unavailable).
     */
    private double computeHeuristicScore(TweetData tweet) {
        double score = 45.0; // neutral baseline
        if (tweet.isAuthorVerified())             score += 20.0; // X verified
        if (tweet.getAuthorFollowers() > 1_000_000) score += 15.0;
        else if (tweet.getAuthorFollowers() > 100_000) score += 10.0;
        else if (tweet.getAuthorFollowers() > 10_000)  score += 5.0;
        if (tweet.getRetweetCount() > 5000)        score += 5.0;
        else if (tweet.getRetweetCount() > 1000)   score += 3.0;
        return Math.min(score, 80.0); // heuristic can't reach tier 1 alone — AI required
    }

    private String buildGrokAuthorityPrompt(TweetData tweet) {
        return String.format("""
            You are Grok AI evaluating the authority and credibility of an X (formerly Twitter) user.
            
            USER: @%s
            DISPLAY NAME: %s
            VERIFIED: %s
            FOLLOWERS: %d
            RECENT TWEET: "%s"
            RETWEET COUNT: %d
            LIKE COUNT: %d
            
            Your task: Analyze this X user's credibility, authority, and potential for propaganda/misinformation.
            
            Consider:
            1. Is this user likely a credible source? (journalist, official, expert, outlet?)
            2. Does the tweet language contain propaganda patterns, emotional manipulation, or false urgency?
            3. What is the overall authority level of this account?
            4. Are there red flags suggesting misinformation?
            
            Score propaganda penalty: 0-50 (0=clean, 50=pure propaganda/misinformation)
            
            Respond ONLY in this JSON format (no markdown):
            {
              "model": "GROK",
              "username": "@%s",
              "authorityScore": <0-100>,
              "propagandaPenalty": <0-50>,
              "accountType": "OFFICIAL | JOURNALIST | EXPERT | MEDIA_OUTLET | INFLUENCER | UNKNOWN",
              "detectedIssues": ["PROPAGANDA" | "EMOTIONAL_MANIPULATION" | "MISINFORMATION" | "FALSE_URGENCY" | "SATIRE"],
              "credibilitySignals": "<key positive or negative signals observed>",
              "summary": "<2-sentence authority assessment>"
            }
            """,
                tweet.getAuthorUsername(), tweet.getAuthorUsername(), tweet.isAuthorVerified(),
                tweet.getAuthorFollowers(), tweet.getText(), tweet.getRetweetCount(), tweet.getLikeCount(),
                tweet.getAuthorUsername());
    }

    private String buildGeminiAuthorityPrompt(TweetData tweet) {
        return String.format("""
            You are Gemini AI cross-validating the authority of an X (formerly Twitter) user.
            
            USER: @%s
            VERIFIED: %s
            FOLLOWERS: %d
            TWEET: "%s"
            
            Your task: Cross-validate this user's authority using Google knowledge graph signals.
            
            Consider:
            1. Is this account known in Google's knowledge graph as a reliable source?
            2. Does the tweet content match known factual patterns or known misinformation templates?
            3. What authority score would you assign based on your knowledge?
            
            Score propaganda penalty: 0-50 (0=clean, 50=confirmed misinformation/propaganda source)
            
            Respond ONLY in this JSON format (no markdown):
            {
              "model": "GEMINI",
              "username": "@%s",
              "authorityScore": <0-100>,
              "propagandaPenalty": <0-50>,
              "detectedIssues": ["PROPAGANDA" | "MISINFORMATION" | "UNVERIFIABLE" | "KNOWN_BIASED_SOURCE"],
              "knowledgeGraphSignals": "<what Google knowledge signals indicate>",
              "summary": "<2-sentence cross-validation result>"
            }
            """,
                tweet.getAuthorUsername(), tweet.isAuthorVerified(),
                tweet.getAuthorFollowers(), tweet.getText(),
                tweet.getAuthorUsername());
    }

    private String safeAiCall(ChatLanguageModel model, String prompt, String modelName) {
        try {
            return model.generate(prompt);
        } catch (Exception e) {
            log.warn("[XUserRanking] {} AI call failed: {}", modelName, e.getMessage());
            return "{\"authorityScore\":50,\"propagandaPenalty\":0,\"detectedIssues\":[]}";
        }
    }

    private double extractScore(String json, String fieldName, double defaultValue) {
        if (json == null) return defaultValue;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has(fieldName)) return node.get(fieldName).asDouble(defaultValue);
        } catch (Exception ignored) {}
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
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
}
