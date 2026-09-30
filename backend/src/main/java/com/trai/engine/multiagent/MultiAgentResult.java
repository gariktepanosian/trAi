package com.trai.engine.multiagent;

/**
 * Final consensus result produced by the dual-agent pipeline.
 * Contains outputs from both agents and the merged final verdict.
 */
public class MultiAgentResult {

    private String claim;
    private String country;
    private String source;
    private AgentOutput agent1;
    private AgentOutput agent2;
    private String finalVerdict;
    private int consensusTrustScore;
    private boolean criticalAlert;
    private String criticalLevel;
    private boolean requiresPushNotification;
    private String timestamp;
    private long processingTimeMs;

    private MultiAgentResult() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MultiAgentResult r = new MultiAgentResult();
        public Builder claim(String v)                     { r.claim = v;                     return this; }
        public Builder country(String v)                   { r.country = v;                   return this; }
        public Builder source(String v)                    { r.source = v;                    return this; }
        public Builder agent1(AgentOutput v)               { r.agent1 = v;                    return this; }
        public Builder agent2(AgentOutput v)               { r.agent2 = v;                    return this; }
        public Builder finalVerdict(String v)              { r.finalVerdict = v;              return this; }
        public Builder consensusTrustScore(int v)          { r.consensusTrustScore = v;       return this; }
        public Builder criticalAlert(boolean v)            { r.criticalAlert = v;             return this; }
        public Builder criticalLevel(String v)             { r.criticalLevel = v;             return this; }
        public Builder requiresPushNotification(boolean v) { r.requiresPushNotification = v;  return this; }
        public Builder timestamp(String v)                 { r.timestamp = v;                 return this; }
        public MultiAgentResult build() { return r; }
    }

    public String getClaim()                  { return claim; }
    public String getCountry()                { return country; }
    public String getSource()                 { return source; }
    public AgentOutput getAgent1()            { return agent1; }
    public AgentOutput getAgent2()            { return agent2; }
    public String getFinalVerdict()           { return finalVerdict; }
    public int getConsensusTrustScore()       { return consensusTrustScore; }
    public boolean isCriticalAlert()          { return criticalAlert; }
    public String getCriticalLevel()          { return criticalLevel; }
    public boolean isRequiresPushNotification() { return requiresPushNotification; }
    public String getTimestamp()              { return timestamp; }
    public long getProcessingTimeMs()         { return processingTimeMs; }

    public void setClaim(String v)                  { this.claim = v; }
    public void setCountry(String v)                { this.country = v; }
    public void setSource(String v)                 { this.source = v; }
    public void setAgent1(AgentOutput v)            { this.agent1 = v; }
    public void setAgent2(AgentOutput v)            { this.agent2 = v; }
    public void setFinalVerdict(String v)           { this.finalVerdict = v; }
    public void setConsensusTrustScore(int v)       { this.consensusTrustScore = v; }
    public void setCriticalAlert(boolean v)         { this.criticalAlert = v; }
    public void setCriticalLevel(String v)          { this.criticalLevel = v; }
    public void setRequiresPushNotification(boolean v) { this.requiresPushNotification = v; }
    public void setTimestamp(String v)              { this.timestamp = v; }
    public void setProcessingTimeMs(long v)         { this.processingTimeMs = v; }
}
