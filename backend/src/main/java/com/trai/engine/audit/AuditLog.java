package com.trai.engine.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Immutable audit log entry. Every API call that processes content
 * creates one of these records.
 */
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    /** The user or partner that triggered the call */
    private String actorId;

    /** LIVE_FACT_CHECK | AI_AUDIT | NEWS_VERIFY | WEBHOOK */
    private String actionType;

    /** Sanitized summary of input (never raw text with PII) */
    private String inputSummary;

    /** Verdict returned */
    private String verdict;

    /** Trust/reliability score 0–100 */
    private Integer trustScore;

    /** Any flags raised by sanitizer or guardrails */
    private List<String> flags;

    /** Was the response blocked by guardrails? */
    private boolean blocked;

    /** Duration of the processing in ms */
    private Long processingTimeMs;

    private Instant createdAt;

    public AuditLog() {
        this.createdAt = Instant.now();
        this.blocked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public Integer getTrustScore() { return trustScore; }
    public void setTrustScore(Integer trustScore) { this.trustScore = trustScore; }

    public List<String> getFlags() { return flags; }
    public void setFlags(List<String> flags) { this.flags = flags; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
