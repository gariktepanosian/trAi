package com.trai.engine.country;

/**
 * Result of a single country monitoring cycle.
 */
public class CountryMonitorResult {

    private final String country;
    private final int tweetsAnalyzed;
    private final String verdict;
    private final int trustScore;
    private final boolean criticalAlert;
    private final String criticalLevel;
    private final String timestamp;

    public CountryMonitorResult(String country, int tweetsAnalyzed, String verdict,
                                 int trustScore, boolean criticalAlert,
                                 String criticalLevel, String timestamp) {
        this.country = country;
        this.tweetsAnalyzed = tweetsAnalyzed;
        this.verdict = verdict;
        this.trustScore = trustScore;
        this.criticalAlert = criticalAlert;
        this.criticalLevel = criticalLevel;
        this.timestamp = timestamp;
    }

    public static CountryMonitorResult noEvents(String country) {
        return new CountryMonitorResult(country, 0, "NO_EVENTS", 0, false, "LOW",
                java.time.Instant.now().toString());
    }

    public String getCountry()        { return country; }
    public int getTweetsAnalyzed()    { return tweetsAnalyzed; }
    public String getVerdict()        { return verdict; }
    public int getTrustScore()        { return trustScore; }
    public boolean isCriticalAlert()  { return criticalAlert; }
    public String getCriticalLevel()  { return criticalLevel; }
    public String getTimestamp()      { return timestamp; }
}
