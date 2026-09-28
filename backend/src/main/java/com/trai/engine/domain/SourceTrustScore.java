package com.trai.engine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "source_trust_scores")
public class SourceTrustScore {

    @Id
    private String id;
    private String sourceUrl;
    private String sourceName;
    private double trustScore; // 0.0 to 100.0
    private int credibilityRank;

    public SourceTrustScore() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public double getTrustScore() { return trustScore; }
    public void setTrustScore(double trustScore) { this.trustScore = trustScore; }

    public int getCredibilityRank() { return credibilityRank; }
    public void setCredibilityRank(int credibilityRank) { this.credibilityRank = credibilityRank; }
}
