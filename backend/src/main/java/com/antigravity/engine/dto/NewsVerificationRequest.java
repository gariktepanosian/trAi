package com.antigravity.engine.dto;

public class NewsVerificationRequest {
    private String text;
    private String sourceUrl;
    private String sourceName;

    public NewsVerificationRequest() {
    }

    public NewsVerificationRequest(String text, String sourceUrl, String sourceName) {
        this.text = text;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }
}
