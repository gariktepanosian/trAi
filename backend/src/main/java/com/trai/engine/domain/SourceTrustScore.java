package com.trai.engine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * SourceTrustScore — Credibility and authority rank for news/information sources.
 *
 * Rank tiers:
 *  TIER_1 (85–100): Institutional — primary source quality (Reuters, AP, BBC)
 *  TIER_2 (70–84):  Reputable — generally reliable
 *  TIER_3 (50–69):  Use with caution — mixed accuracy record
 *  TIER_4 (25–49):  High bias detected — significant propaganda patterns
 *  TIER_5 (0–24):   Do not rely — consistent misinformation
 *
 * AI validation flow:
 *  1. SourceAuthorityService submits the source URL to Grok + Gemini.
 *  2. AIs check for propaganda patterns, known misinformation, authority signals.
 *  3. propagandaPoints are accumulated per AI check (higher = worse).
 *  4. trustScore is adjusted based on AI findings and audit log accuracy data.
 *  5. Daily scheduler recalculates credibilityRank for all sources.
 */
@Document(collection = "source_trust_scores")
public class SourceTrustScore {

    @Id
    private String id;
    private String sourceUrl;
    private String sourceName;

    /** Composite credibility score 0.0–100.0 (higher = more trustworthy). */
    private double trustScore;

    /** Integer rank position — 1 = most trusted. Recalculated daily. */
    private int credibilityRank;

    /** Tier label derived from trustScore. */
    private String credibilityTier;

    // ── AI-Validated Fields ───────────────────────────────────────────────────

    /** Accumulated propaganda/misinformation penalty points (0–50, higher = worse). */
    private double propagandaPoints;

    /** Total articles TrAI has verified from this source. */
    private int totalArticlesChecked;

    /** Articles verified as accurate by AI consensus. */
    private int accurateArticleCount;

    /** Articles flagged as misleading or false by AI consensus. */
    private int misinformationArticleCount;

    /** AI-detected issues (e.g. "PROPAGANDA", "SELECTIVE_REPORTING", "EMOTIONAL_LANGUAGE"). */
    private List<String> detectedIssues;

    /** Which AI models last verified this source ("GROK", "GEMINI", "DUAL"). */
    private String aiVerifiedBy;

    /** ISO-8601 timestamp of last AI authority check. */
    private String lastCheckedAt;

    /** Whether real analytics have been collected (false = seed data only). */
    private boolean analyticsBootstrapped;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public SourceTrustScore() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }

    public String getSourceUrl()                   { return sourceUrl; }
    public void setSourceUrl(String v)             { this.sourceUrl = v; }

    public String getSourceName()                  { return sourceName; }
    public void setSourceName(String v)            { this.sourceName = v; }

    public double getTrustScore()                  { return trustScore; }
    public void setTrustScore(double v)            { this.trustScore = v; }

    public int getCredibilityRank()                { return credibilityRank; }
    public void setCredibilityRank(int v)          { this.credibilityRank = v; }

    public String getCredibilityTier()             { return credibilityTier; }
    public void setCredibilityTier(String v)       { this.credibilityTier = v; }

    public double getPropagandaPoints()            { return propagandaPoints; }
    public void setPropagandaPoints(double v)      { this.propagandaPoints = v; }

    public int getTotalArticlesChecked()           { return totalArticlesChecked; }
    public void setTotalArticlesChecked(int v)     { this.totalArticlesChecked = v; }

    public int getAccurateArticleCount()           { return accurateArticleCount; }
    public void setAccurateArticleCount(int v)     { this.accurateArticleCount = v; }

    public int getMisinformationArticleCount()     { return misinformationArticleCount; }
    public void setMisinformationArticleCount(int v) { this.misinformationArticleCount = v; }

    public List<String> getDetectedIssues()        { return detectedIssues; }
    public void setDetectedIssues(List<String> v)  { this.detectedIssues = v; }

    public String getAiVerifiedBy()                { return aiVerifiedBy; }
    public void setAiVerifiedBy(String v)          { this.aiVerifiedBy = v; }

    public String getLastCheckedAt()               { return lastCheckedAt; }
    public void setLastCheckedAt(String v)         { this.lastCheckedAt = v; }

    public boolean isAnalyticsBootstrapped()       { return analyticsBootstrapped; }
    public void setAnalyticsBootstrapped(boolean v){ this.analyticsBootstrapped = v; }

    /** ISO-8601 timestamp when TrAI first encountered this source. */
    private String firstSeenAt;

    public String getFirstSeenAt()                 { return firstSeenAt; }
    public void setFirstSeenAt(String v)           { this.firstSeenAt = v; }
}
