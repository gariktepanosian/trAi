package com.trai.engine.webhook;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Persisted record of every inbound B2B webhook request.
 */
@Document(collection = "webhook_requests")
public class WebhookRequest {

    @Id
    private String id;

    /** Partner who sent this request */
    private String partnerId;

    /** NEWS_VERIFY | AI_AUDIT | LIVE_FACT_CHECK */
    private String requestType;

    /** Raw inbound payload */
    private Map<String, Object> payload;

    /** RECEIVED | PROCESSING | COMPLETED | FAILED */
    private String status;

    /** Result from the internal service */
    private Map<String, Object> result;

    private Instant receivedAt;
    private Instant completedAt;

    public WebhookRequest() {
        this.receivedAt = Instant.now();
        this.status = "RECEIVED";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getResult() { return result; }
    public void setResult(Map<String, Object> result) { this.result = result; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
