package com.trai.engine.alert;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "platform_alerts")
public class PlatformAlert {

    @Id
    private String id;
    private String severity; // CRITICAL, HIGH, INFO
    private String eventType; // PROMPT_INJECTION, HIGH_RISK_GLITCH, LOW_TRUST_CLAIM, SYSTEM_HALTED
    private String message;
    private String source;
    private boolean resolved;
    private Instant createdAt;

    public PlatformAlert() {
        this.createdAt = Instant.now();
        this.resolved = false;
    }

    public PlatformAlert(String severity, String eventType, String message, String source) {
        this();
        this.severity = severity;
        this.eventType = eventType;
        this.message = message;
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
