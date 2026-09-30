package com.trai.engine.country;

import com.trai.engine.alert.PlatformAlert;
import com.trai.engine.alert.PlatformAlertService;
import com.trai.engine.multiagent.MultiAgentResult;
import com.trai.engine.multiagent.MultiAgentValidationService;
import com.trai.engine.notifications.PushNotificationService;
import com.trai.engine.twitter.TweetData;
import com.trai.engine.twitter.TwitterApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CountryMonitoringService — Core monitoring pipeline.
 *
 * Flow:
 *  1. Users register their country preference (stored in countrySubscribers map)
 *  2. Every 10 minutes: for each monitored country, fetch real tweets from Twitter/X
 *  3. Each tweet batch is passed to the multi-agent pipeline (Grok + Gemini + ChatGPT)
 *  4. If consensus verdict = CRITICAL/HIGH + trust score > threshold:
 *     → Create PlatformAlert
 *     → Send push notification to all subscribers of that country
 */
@Service
public class CountryMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(CountryMonitoringService.class);

    // Map: countryName → Set of FCM device tokens (user push tokens)
    // In production this would be stored in MongoDB per user document
    private final ConcurrentHashMap<String, Set<String>> countrySubscribers = new ConcurrentHashMap<>();

    private final TwitterApiService twitterApiService;
    private final MultiAgentValidationService multiAgentValidationService;
    private final PlatformAlertService alertService;
    private final PushNotificationService pushNotificationService;

    public CountryMonitoringService(
            TwitterApiService twitterApiService,
            MultiAgentValidationService multiAgentValidationService,
            PlatformAlertService alertService,
            PushNotificationService pushNotificationService) {
        this.twitterApiService = twitterApiService;
        this.multiAgentValidationService = multiAgentValidationService;
        this.alertService = alertService;
        this.pushNotificationService = pushNotificationService;
    }

    // ─── User Subscription Management ────────────────────────────────────────

    /**
     * Register a user's FCM token for a country.
     * Called when the user selects their country in the Flutter app.
     */
    public void subscribeUserToCountry(String country, String fcmToken) {
        String normalizedCountry = country.trim();
        countrySubscribers.computeIfAbsent(normalizedCountry, k -> ConcurrentHashMap.newKeySet())
                .add(fcmToken);
        log.info("[CountryMonitor] User subscribed to country={}, total subscribers={}",
                normalizedCountry, countrySubscribers.get(normalizedCountry).size());
    }

    /**
     * Unsubscribe a user from a country (e.g. when they change their country).
     */
    public void unsubscribeUserFromCountry(String country, String fcmToken) {
        Set<String> tokens = countrySubscribers.get(country.trim());
        if (tokens != null) {
            tokens.remove(fcmToken);
        }
    }

    /**
     * Get all currently monitored countries.
     */
    public Set<String> getMonitoredCountries() {
        return countrySubscribers.keySet();
    }

    /**
     * Get subscriber count for a country.
     */
    public int getSubscriberCount(String country) {
        Set<String> tokens = countrySubscribers.get(country.trim());
        return tokens != null ? tokens.size() : 0;
    }

    // ─── Scheduled Twitter Monitoring ────────────────────────────────────────

    /**
     * Main monitoring loop — runs every 10 minutes.
     * For each subscribed country, fetches tweets and runs multi-agent validation.
     */
    @Scheduled(fixedRate = 600_000, initialDelay = 30_000)
    public void monitorAllCountries() {
        if (countrySubscribers.isEmpty()) {
            log.debug("[CountryMonitor] No countries subscribed — skipping cycle");
            return;
        }

        log.info("[CountryMonitor] Starting monitoring cycle for {} countries",
                countrySubscribers.size());

        for (String country : countrySubscribers.keySet()) {
            try {
                monitorCountry(country);
            } catch (Exception e) {
                log.error("[CountryMonitor] Error monitoring country={}: {}", country, e.getMessage());
            }
        }
    }

    /**
     * Monitor a single country: fetch tweets → validate → alert if critical.
     */
    public CountryMonitorResult monitorCountry(String country) {
        log.info("[CountryMonitor] Monitoring country={}", country);

        // Step 1: Fetch real tweets from Twitter/X API
        List<TweetData> tweets = twitterApiService.searchCriticalTweets(country, 20);
        if (tweets.isEmpty()) {
            log.info("[CountryMonitor] No tweets found for country={}", country);
            return CountryMonitorResult.noEvents(country);
        }

        // Step 2: Aggregate tweet texts with virality weight for AI analysis
        // Higher-virality tweets (verified authors, high RT) get more weight
        String aggregatedClaim = buildAggregatedClaim(tweets, country);

        // Step 3: Run dual-agent multi-model validation pipeline
        MultiAgentResult validationResult = multiAgentValidationService
                .validateWithDualAgent(aggregatedClaim, country, "Twitter/X");

        log.info("[CountryMonitor] Validation complete for country={} | verdict={} score={} critical={}",
                country, validationResult.getFinalVerdict(),
                validationResult.getConsensusTrustScore(),
                validationResult.isCriticalAlert());

        // Step 4: If critical and trusted enough, trigger alert + push notification
        if (validationResult.isCriticalAlert() && validationResult.getConsensusTrustScore() >= 55) {
            triggerCriticalAlert(country, validationResult, tweets);
        }

        return new CountryMonitorResult(
                country,
                tweets.size(),
                validationResult.getFinalVerdict(),
                validationResult.getConsensusTrustScore(),
                validationResult.isCriticalAlert(),
                validationResult.getCriticalLevel(),
                validationResult.getTimestamp()
        );
    }

    // ─── Alert + Push Notification ────────────────────────────────────────────

    private void triggerCriticalAlert(String country, MultiAgentResult result, List<TweetData> tweets) {
        String severity = "CRITICAL".equals(result.getCriticalLevel()) ? "CRITICAL" : "HIGH";
        String topTweet = tweets.isEmpty() ? "No tweet text" : tweets.get(0).getText();

        // Create platform alert
        PlatformAlert alert = alertService.triggerAlert(
                severity,
                "COUNTRY_CRITICAL_EVENT",
                String.format("[%s] %s — Consensus: %s (Trust: %d%%)",
                        country, topTweet.substring(0, Math.min(120, topTweet.length())),
                        result.getFinalVerdict(), result.getConsensusTrustScore()),
                "Twitter/X via MultiAgent [" + result.getAgent1().getAgentId() +
                " + " + result.getAgent2().getAgentId() + "]"
        );

        // Send push notification to all subscribers of this country
        Set<String> tokens = countrySubscribers.getOrDefault(country, Set.of());
        if (!tokens.isEmpty()) {
            String title = severity + ": " + country;
            String body = buildPushNotificationBody(result, topTweet);
            pushNotificationService.sendToMultipleDevices(
                    new ArrayList<>(tokens), title, body,
                    Map.of(
                            "country", country,
                            "verdict", result.getFinalVerdict(),
                            "trustScore", String.valueOf(result.getConsensusTrustScore()),
                            "criticalLevel", result.getCriticalLevel(),
                            "alertId", alert.getId() != null ? alert.getId() : "unknown",
                            "type", "COUNTRY_ALERT"
                    )
            );
            log.info("[CountryMonitor] Push notification sent to {} devices for country={}",
                    tokens.size(), country);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Aggregate tweet texts into a single claim for AI analysis.
     * Weights high-virality tweets (verified, high engagement) more heavily.
     */
    private String buildAggregatedClaim(List<TweetData> tweets, String country) {
        StringBuilder sb = new StringBuilder();
        sb.append("COUNTRY: ").append(country).append("\n");
        sb.append("REAL TWITTER/X REPORTS (").append(tweets.size()).append(" tweets):\n\n");

        // Sort by virality weight descending
        tweets.sort((a, b) -> Double.compare(b.getViralityWeight(), a.getViralityWeight()));

        int i = 1;
        for (TweetData tweet : tweets) {
            sb.append(i++).append(". ");
            if (tweet.isAuthorVerified()) sb.append("[VERIFIED @").append(tweet.getAuthorUsername()).append("] ");
            else sb.append("[@").append(tweet.getAuthorUsername()).append("] ");
            sb.append(tweet.getText());
            sb.append(" [RT:").append(tweet.getRetweetCount())
              .append(" LIKES:").append(tweet.getLikeCount()).append("]");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildPushNotificationBody(MultiAgentResult result, String topTweet) {
        String preview = topTweet.length() > 80 ? topTweet.substring(0, 80) + "..." : topTweet;
        return String.format("Verified by Grok+Gemini+ChatGPT — %s (%d%% confidence): %s",
                result.getFinalVerdict(), result.getConsensusTrustScore(), preview);
    }
}
