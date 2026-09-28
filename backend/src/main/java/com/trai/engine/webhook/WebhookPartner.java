package com.trai.engine.webhook;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Registered B2B partner. Stores API credentials and webhook callback URL.
 */
@Document(collection = "webhook_partners")
public class WebhookPartner {

    @Id
    private String id;

    private String companyName;

    @Indexed(unique = true)
    private String email;

    /** HMAC-SHA256 signing secret for verifying inbound requests */
    private String signingSecret;

    /** Optional: URL to POST results back asynchronously */
    private String callbackUrl;

    private boolean active;
    private Instant registeredAt;

    public WebhookPartner() {
        this.active = true;
        this.registeredAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSigningSecret() { return signingSecret; }
    public void setSigningSecret(String signingSecret) { this.signingSecret = signingSecret; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}
