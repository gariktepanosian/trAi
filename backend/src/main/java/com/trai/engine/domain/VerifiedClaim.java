package com.trai.engine.domain;

public class VerifiedClaim {
    private String source;
    private String claim;

    public VerifiedClaim() {}

    public VerifiedClaim(String source, String claim) {
        this.source = source;
        this.claim = claim;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getClaim() { return claim; }
    public void setClaim(String claim) { this.claim = claim; }
}
