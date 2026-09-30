package com.trai.engine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * XUserRankScore — Authority & trust rank for X (formerly Twitter) user accounts.
 *
 * How it works:
 *  1. When TrAI processes tweets from a user, XUserRankingService evaluates the user.
 *  2. Grok AI checks the user's post history for propaganda patterns, misinformation,
 *     or authoritative/credible signals, producing a propagandaPoints penalty.
 *  3. Gemini cross-validates the user's authority against Google knowledge signals.
 *  4. A final authorityScore (0–100) and authorityRank (1 = most trusted) are computed.
 *  5. Scores are persisted here and used by TwitterApiService to weight viralityWeight.
 *  6. The scheduler recalculates ranks daily as more data accumulates.
 *
 * Tiers:
 *  Tier 1 (85–100): Highly authoritative — government, verified journalist, major outlet
 *  Tier 2 (70–84):  Reputable — consistent factual posting
 *  Tier 3 (50–69):  Use with caution — mixed record
 *  Tier 4 (25–49):  High bias or frequent misinformation detected
 *  Tier 5 (0–24):   Unreliable — consistent propaganda or false content
 */
@Document(collection = "x_user_rank_scores")
public class XUserRankScore {

    @Id
    private String id;                        // username (lowercase, no @)

    @Indexed(unique = true)
    private String username;                  // X handle without @

    private String displayName;               // X display name

    private boolean verified;                 // X verified / Blue check

    private int followerCount;                // current follower count

    // ── Authority Score ───────────────────────────────────────────────────────

    private double authorityScore;            // 0.0–100.0 composite score

    private int authorityRank;                // integer rank, 1 = most trusted

    private String authorityTier;             // TIER_1 … TIER_5 label

    // ── AI-Validated Penalty Points ───────────────────────────────────────────

    private double propagandaPoints;          // cumulative penalty (higher = worse)

    private int misinformationCount;          // number of posts flagged as false/misleading

    private int verifiedAccurateCount;        // number of posts confirmed accurate

    private List<String> detectedIssues;      // ["PROPAGANDA", "EMOTIONAL_MANIPULATION", ...]

    // ── Grok AI Assessment ────────────────────────────────────────────────────

    private String grokAssessment;            // last Grok authority analysis (JSON)

    private double grokScore;                 // 0–100 score from Grok last run

    // ── Gemini AI Cross-Validation ────────────────────────────────────────────

    private String geminiAssessment;          // last Gemini authority analysis (JSON)

    private double geminiScore;               // 0–100 score from Gemini last run

    // ── Metadata ──────────────────────────────────────────────────────────────

    private String lastCheckedAt;             // ISO-8601 timestamp of last AI check

    private String firstSeenAt;               // ISO-8601 when first encountered by TrAI

    private int totalPostsAnalyzed;           // total X posts TrAI has ever processed

    private boolean analyticsBootstrapped;    // false = only seeded, true = real data collected

    // ─── Constructors ─────────────────────────────────────────────────────────

    public XUserRankScore() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId()                           { return id; }
    public void setId(String id)                    { this.id = id; }

    public String getUsername()                     { return username; }
    public void setUsername(String username)        { this.username = username; }

    public String getDisplayName()                  { return displayName; }
    public void setDisplayName(String displayName)  { this.displayName = displayName; }

    public boolean isVerified()                     { return verified; }
    public void setVerified(boolean verified)       { this.verified = verified; }

    public int getFollowerCount()                   { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    public double getAuthorityScore()               { return authorityScore; }
    public void setAuthorityScore(double score)     { this.authorityScore = score; }

    public int getAuthorityRank()                   { return authorityRank; }
    public void setAuthorityRank(int rank)          { this.authorityRank = rank; }

    public String getAuthorityTier()                { return authorityTier; }
    public void setAuthorityTier(String tier)       { this.authorityTier = tier; }

    public double getPropagandaPoints()             { return propagandaPoints; }
    public void setPropagandaPoints(double pts)     { this.propagandaPoints = pts; }

    public int getMisinformationCount()             { return misinformationCount; }
    public void setMisinformationCount(int count)   { this.misinformationCount = count; }

    public int getVerifiedAccurateCount()           { return verifiedAccurateCount; }
    public void setVerifiedAccurateCount(int count) { this.verifiedAccurateCount = count; }

    public List<String> getDetectedIssues()         { return detectedIssues; }
    public void setDetectedIssues(List<String> issues) { this.detectedIssues = issues; }

    public String getGrokAssessment()               { return grokAssessment; }
    public void setGrokAssessment(String v)         { this.grokAssessment = v; }

    public double getGrokScore()                    { return grokScore; }
    public void setGrokScore(double v)              { this.grokScore = v; }

    public String getGeminiAssessment()             { return geminiAssessment; }
    public void setGeminiAssessment(String v)       { this.geminiAssessment = v; }

    public double getGeminiScore()                  { return geminiScore; }
    public void setGeminiScore(double v)            { this.geminiScore = v; }

    public String getLastCheckedAt()                { return lastCheckedAt; }
    public void setLastCheckedAt(String v)          { this.lastCheckedAt = v; }

    public String getFirstSeenAt()                  { return firstSeenAt; }
    public void setFirstSeenAt(String v)            { this.firstSeenAt = v; }

    public int getTotalPostsAnalyzed()              { return totalPostsAnalyzed; }
    public void setTotalPostsAnalyzed(int count)    { this.totalPostsAnalyzed = count; }

    public boolean isAnalyticsBootstrapped()        { return analyticsBootstrapped; }
    public void setAnalyticsBootstrapped(boolean v) { this.analyticsBootstrapped = v; }
}
