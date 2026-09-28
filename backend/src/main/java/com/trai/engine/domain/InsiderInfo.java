package com.trai.engine.domain;

public class InsiderInfo {
    private String status;
    private String detail;
    private String potentialImpact;

    public InsiderInfo() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getPotentialImpact() { return potentialImpact; }
    public void setPotentialImpact(String potentialImpact) { this.potentialImpact = potentialImpact; }
}
