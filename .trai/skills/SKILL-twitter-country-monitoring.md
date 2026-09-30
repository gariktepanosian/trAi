# SKILL: X (formerly Twitter) Monitoring & Country Alerting
# TrAI Platform — Real-Time Country Event Detection

## What this skill covers

How TrAI monitors X (formerly Twitter) for critical events per country, how users subscribe,
how the scheduler triggers the multi-agent pipeline, how alerts are generated,
and how X user authority scores are computed and stored.

---

## File Locations

```
backend/src/main/java/com/trai/engine/twitter/
├── TwitterApiService.java     # X API v2 client (class name kept for backwards compat)
├── TweetData.java             # Post data model (includes authorityScore)
└── XUserRankingService.java   # X user authority ranking engine (NEW)

backend/src/main/java/com/trai/engine/domain/
└── XUserRankScore.java        # MongoDB document for X user authority ranks (NEW)

backend/src/main/java/com/trai/engine/repository/
└── XUserRankScoreRepository.java  # MongoDB repository for X user ranks (NEW)

backend/src/main/java/com/trai/engine/country/
├── CountryMonitoringService.java  # Core monitoring orchestrator + scheduler
├── CountryController.java         # REST API for subscribe/unsubscribe
└── CountryMonitorResult.java      # Single monitoring cycle result
```

---

## Full Data Flow

```
User opens app → selects country in Settings screen
        │
        └── POST /api/v1/country/subscribe
            { "country": "Armenia", "fcmToken": "<device-token>" }
                │
                └── CountryMonitoringService.subscribeUserToCountry()
                    Stored in: ConcurrentHashMap<country, Set<fcmToken>>

Every 10 minutes → @Scheduled(fixedRate=600_000)
        │
        └── CountryMonitoringService.monitorAllCountries()
            For each subscribed country:
                │
                ├── 1. TwitterApiService.searchCriticalTweets(country, 20)
                │       X API v2: GET https://api.twitter.com/2/tweets/search/recent
                │       Query: (country) (tornado OR war OR earthquake OR...) -is:retweet
                │
                ├── 2. Per post: XUserRankingService.evaluateAndRankUser(tweet)
                │       → Grok + Gemini assess author authority (parallel)
                │       → authorityScore (0–100) + propagandaPoints persisted to MongoDB
                │       → authorityScore fed into viralityWeight calculation
                │
                ├── 3. Build aggregated claim string
                │       Sorted by viralityWeight desc
                │       Includes: @username, verified status, RT count, text
                │
                ├── 4. MultiAgentValidationService.validateWithDualAgent(claim, country, "X")
                │       → Runs full dual-agent parallel pipeline (see SKILL-multi-agent-pipeline.md)
                │
                └── 5. If criticalAlert=true AND trustScore >= 55:
                        ├── PlatformAlertService.triggerAlert(severity, type, message, source)
                        └── PushNotificationService.sendToMultipleDevices(tokens, title, body, data)
```

---

## X API v2 Integration

### Endpoint used
`GET https://api.twitter.com/2/tweets/search/recent`

Note: X has not migrated this endpoint to a new domain. The API domain remains `api.twitter.com/2`.
The xAI Grok AI endpoint uses `api.x.ai/v1` (separate platform).

### Authentication
Bearer Token (OAuth 2.0 App-only) — stored as `TWITTER_BEARER_TOKEN` env var.
**Never** use user-level OAuth 1.0a for this feature.

### Query construction
```java
// Built in TwitterApiService.buildCriticalQuery()
String query = String.format(
    "(%s) (tornado OR earthquake OR war OR explosion OR attack OR flood " +
    "OR missile OR evacuation OR emergency OR crisis OR breaking OR urgent) " +
    "-is:retweet lang:en",
    country);
```

To add new critical keywords, edit `buildCriticalQuery()` in `TwitterApiService.java`.
Add the keyword to the OR list. Keep the `-is:retweet` filter always.

### Fields requested
```
tweet.fields=text,created_at,author_id,public_metrics,lang,geo
expansions=author_id
user.fields=verified,username,name,public_metrics
```

### Rate limit awareness
- X API Basic tier: 10,000 posts/month search limit
- Each monitoring cycle fetches max 20 posts per country
- With 10 countries monitored: 200 posts × 144 cycles/day = 28,800 posts/day
- **Academic/Elevated access required** for production at scale

---

## X User Authority Ranking

Every post's author is evaluated by `XUserRankingService` immediately after fetch.
This is a new compounding data moat: the more posts TrAI processes, the more accurate
the X user authority index becomes.

### `XUserRankScore` fields (MongoDB `x_user_rank_scores` collection)

```java
XUserRankScore {
    String id;                    // username (lowercase, no @)
    String username;              // X handle without @
    String displayName;           // X display name
    boolean verified;             // X verified / Blue check
    int followerCount;            // current follower count

    double authorityScore;        // 0.0–100.0 composite score
    int authorityRank;            // integer rank, 1 = most trusted
    String authorityTier;         // TIER_1 … TIER_5

    double propagandaPoints;      // accumulated penalty (higher = worse, max 50)
    int misinformationCount;      // posts flagged as false/misleading
    int verifiedAccurateCount;    // posts confirmed accurate

    List<String> detectedIssues;  // ["PROPAGANDA", "EMOTIONAL_MANIPULATION", ...]
    double grokScore;             // 0–100 from Grok last run
    double geminiScore;           // 0–100 from Gemini last run

    String lastCheckedAt;         // ISO-8601 last AI check
    String firstSeenAt;           // ISO-8601 first seen by TrAI
    int totalPostsAnalyzed;       // total X posts TrAI processed for this user
    boolean analyticsBootstrapped; // true = >= 3 posts analyzed (real data)
}
```

### Authority Tier scale

| authorityScore | authorityTier | Description |
|---|---|---|
| 85–100 | TIER_1 | Government, major verified journalists, institutional outlets |
| 70–84 | TIER_2 | Consistent, reputable X posters |
| 50–69 | TIER_3 | Mixed record — use with caution |
| 25–49 | TIER_4 | High bias or detected misinformation |
| 0–24 | TIER_5 | Unreliable — consistent propaganda |

### Analytics bootstrapping

- DB starts empty. Every processed post adds data.
- `analyticsBootstrapped` becomes `true` when `totalPostsAnalyzed >= 3`.
- Even on first encounter, the initial AI assessment produces a meaningful score.
- Daily scheduler (`TrustScoreRecalculationScheduler`) re-ranks all users once per day.

---

## `TweetData` Fields

```java
TweetData {
    String id;               // Post ID from API
    String text;             // Full post text
    String authorUsername;   // @handle (no @)
    boolean authorVerified;  // X Blue / legacy verified
    int authorFollowers;     // followers_count
    int retweetCount;        // retweet_count
    int likeCount;           // like_count
    String country;          // Which country this was fetched for
    String lang;             // Post language (from API)
    String createdAt;        // ISO-8601 timestamp
    double viralityWeight;   // Computed: base=1.0 + verified(+2.0) + RT>1000(+1.5) + followers>100k(+1.0) + authority bonus
    double authorityScore;   // X user authority score from XUserRankingService (0–100)
}
```

**Virality weight** determines sort order when building the aggregated claim.
Higher-weight posts appear first and get more AI attention.

---

## Virality Weight Formula (Updated)

```java
double viralityWeight = 1.0;
if (tweet.isAuthorVerified())              viralityWeight += 2.0;   // Official/verified account
if (tweet.getRetweetCount() > 1000)        viralityWeight += 1.5;   // Viral spread
if (tweet.getAuthorFollowers() > 100_000)  viralityWeight += 1.0;   // High-reach account
viralityWeight += (authorityScore / 100.0) * 1.5;                   // AI authority bonus (0–1.5)
// Max possible: ~7.0 (all conditions + tier-1 authority)
```

To tune this formula, edit `TwitterApiService.parsePosts()`.

---

## Country Subscription Storage

**Current implementation:** In-memory `ConcurrentHashMap<String, Set<String>>`
- Key: country name (normalized, trimmed)
- Value: `ConcurrentHashMap.newKeySet()` of FCM device tokens

**Production limitation:** This map is lost on restart. Each mobile client must
re-subscribe when the app opens (handled in `HomeScreen.initState()` via `NotificationService.initialize()`).

**Migration path:** Move to MongoDB when persistent subscriptions are needed:
- Create `CountrySubscription` `@Document(collection = "country_subscriptions")`
- Fields: `country`, `fcmToken`, `userId`, `subscribedAt`, `active`
- Replace `ConcurrentHashMap` with `CountrySubscriptionRepository`

---

## REST Endpoints

### `POST /api/v1/country/subscribe`
```json
Request:  { "country": "Armenia", "fcmToken": "device-fcm-registration-token" }
Response: { "status": "SUBSCRIBED", "country": "Armenia", "subscriberCount": 42, "message": "..." }
```

### `POST /api/v1/country/unsubscribe`
```json
Request:  { "country": "Armenia", "fcmToken": "device-fcm-registration-token" }
Response: { "status": "UNSUBSCRIBED", "country": "Armenia" }
```

### `GET /api/v1/country/monitored`
```json
Response: { "countries": ["Armenia", "Ukraine", "Japan"], "total": 3 }
```

### `POST /api/v1/country/trigger-check` (admin manual trigger)
```json
Request:  { "country": "Armenia" }
Response: {
  "country": "Armenia",
  "tweetsAnalyzed": 15,
  "verdict": "LIKELY_TRUE",
  "trustScore": 72,
  "criticalAlert": false,
  "criticalLevel": "MEDIUM",
  "timestamp": "2026-09-29T14:32:01Z"
}
```

---

## Mock Mode (Dev / No API Key)

When `TWITTER_BEARER_TOKEN` is not set or equals `your-twitter-bearer-token`,
`TwitterApiService.searchCriticalTweets()` returns 3 mock posts:
1. A breaking alert from a verified account (high virality weight: 5.775, authorityScore: 85)
2. An unverified report (medium virality: 1.87, authorityScore: 58)
3. A calming counter-report (low virality: 1.75, authorityScore: 50)

This ensures the full pipeline runs and can be tested end-to-end without a real API key.

---

## Adding a New Critical Keyword

1. Edit `buildCriticalQuery()` in `TwitterApiService.java`
2. Add to the OR list: `OR <new_keyword>`
3. If keyword needs to trigger `criticalAlert` detection in `MultiAgentValidationService`,
   add it to the keyword list in `detectCriticalAlert()` method
4. Update `tests/api/country-monitoring.spec.ts` to include a test with the new keyword

---

## Playwright Test Coverage Trigger

After any change to `CountryController`, `CountryMonitoringService`, `TwitterApiService`,
or `XUserRankingService`:

- `tests/api/country-subscribe.spec.ts` — test subscribe + unsubscribe + get monitored
- `tests/api/country-trigger-check.spec.ts` — test manual trigger-check with mocked X + mocked AI
- Assert: response contains `tweetsAnalyzed`, `verdict`, `trustScore`, `criticalAlert`
- Test critical path: when posts contain "tornado" → `criticalAlert: true`
- Test below-threshold path: when trust score < 55 → `criticalAlert: false` even with keywords
- Test X user rank: verify `authorityScore` is set on `TweetData` objects after processing
