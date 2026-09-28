package com.antigravity.engine.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "normalized_news")
public class NormalizedNews {

    @Id
    private String id;
    private String eventId;
    private String normalizedTitle;
    private List<String> verifiedFacts;
    private List<VerifiedClaim> unverifiedClaims;
    private List<InsiderInfo> insiderInfo;
    private String dryAnalytics;
    private List<String> sourceLinks;
    private List<String> searchTags;
    private Instant createdAt;

    public NormalizedNews() {
        this.createdAt = Instant.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public void setNormalizedTitle(String normalizedTitle) {
        this.normalizedTitle = normalizedTitle;
    }

    public List<String> getVerifiedFacts() {
        return verifiedFacts;
    }

    public void setVerifiedFacts(List<String> verifiedFacts) {
        this.verifiedFacts = verifiedFacts;
    }

    public List<VerifiedClaim> getUnverifiedClaims() {
        return unverifiedClaims;
    }

    public void setUnverifiedClaims(List<VerifiedClaim> unverifiedClaims) {
        this.unverifiedClaims = unverifiedClaims;
    }

    public List<InsiderInfo> getInsiderInfo() {
        return insiderInfo;
    }

    public void setInsiderInfo(List<InsiderInfo> insiderInfo) {
        this.insiderInfo = insiderInfo;
    }

    public String getDryAnalytics() {
        return dryAnalytics;
    }

    public void setDryAnalytics(String dryAnalytics) {
        this.dryAnalytics = dryAnalytics;
    }

    public List<String> getSourceLinks() {
        return sourceLinks;
    }

    public void setSourceLinks(List<String> sourceLinks) {
        this.sourceLinks = sourceLinks;
    }

    public List<String> getSearchTags() {
        return searchTags;
    }

    public void setSearchTags(List<String> searchTags) {
        this.searchTags = searchTags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
