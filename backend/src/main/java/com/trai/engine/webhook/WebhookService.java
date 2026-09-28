package com.trai.engine.webhook;

import com.trai.engine.dto.AiGlitchCheckRequest;
import com.trai.engine.dto.LiveStatementRequest;
import com.trai.engine.dto.NewsVerificationRequest;
import com.trai.engine.service.TrustVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Core B2B Webhook service.
 *
 * Responsibilities:
 *  1. Verify HMAC-SHA256 signature of inbound requests
 *  2. Classify request type and route to the correct internal service
 *  3. Persist the request record and result to MongoDB
 *  4. Register new B2B partners
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final TrustVerificationService trustService;
    private final WebhookRequestRepository requestRepository;
    private final WebhookPartnerRepository partnerRepository;

    public WebhookService(TrustVerificationService trustService,
                          WebhookRequestRepository requestRepository,
                          WebhookPartnerRepository partnerRepository) {
        this.trustService = trustService;
        this.requestRepository = requestRepository;
        this.partnerRepository = partnerRepository;
    }

    /**
     * Verify HMAC-SHA256 signature.
     * Expected header value: hex(HMAC-SHA256(signingSecret, requestBodyBytes))
     */
    public boolean verifySignature(String partnerId, String rawBody, String signatureHeader) {
        return partnerRepository.findById(partnerId).map(partner -> {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(
                        partner.getSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] expected = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
                String expectedHex = HexFormat.of().formatHex(expected);
                return expectedHex.equalsIgnoreCase(signatureHeader);
            } catch (Exception e) {
                log.error("HMAC verification error: {}", e.getMessage());
                return false;
            }
        }).orElse(false);
    }

    /**
     * Process an inbound webhook payload.
     * Routes to: NEWS_VERIFY | AI_AUDIT | LIVE_FACT_CHECK
     */
    public Map<String, Object> processIngest(String partnerId, String requestType,
                                              Map<String, Object> payload) {
        // Persist the incoming request
        WebhookRequest record = new WebhookRequest();
        record.setPartnerId(partnerId);
        record.setRequestType(requestType);
        record.setPayload(payload);
        record.setStatus("PROCESSING");
        record = requestRepository.save(record);

        Map<String, Object> result;
        try {
            result = switch (requestType.toUpperCase()) {
                case "NEWS_VERIFY" -> {
                    NewsVerificationRequest req = new NewsVerificationRequest(
                            (String) payload.getOrDefault("text", ""),
                            (String) payload.getOrDefault("sourceUrl", ""),
                            (String) payload.getOrDefault("sourceName", "B2B Partner"));
                    yield trustService.verifyNewsArticle(req);
                }
                case "AI_AUDIT" -> {
                    AiGlitchCheckRequest req = new AiGlitchCheckRequest(
                            (String) payload.getOrDefault("prompt", ""),
                            (String) payload.getOrDefault("aiResponse", ""),
                            (String) payload.getOrDefault("modelName", "Unknown"));
                    yield trustService.verifyAiGlitch(req);
                }
                case "LIVE_FACT_CHECK" -> {
                    LiveStatementRequest req = new LiveStatementRequest(
                            (String) payload.getOrDefault("speaker", "Unknown"),
                            (String) payload.getOrDefault("statement", ""),
                            (String) payload.getOrDefault("mediaSource", "B2B Feed"));
                    yield trustService.verifyLiveStatement(req);
                }
                default -> Map.of("error", "Unknown requestType: " + requestType,
                        "supported", "NEWS_VERIFY, AI_AUDIT, LIVE_FACT_CHECK");
            };

            record.setStatus("COMPLETED");
            record.setResult(result);
            record.setCompletedAt(Instant.now());

        } catch (Exception e) {
            log.error("Webhook processing failed for partner {}: {}", partnerId, e.getMessage());
            result = Map.of("error", "Processing failed", "detail", e.getMessage());
            record.setStatus("FAILED");
            record.setResult(result);
            record.setCompletedAt(Instant.now());
        }

        requestRepository.save(record);

        // Return result plus the request ID for async status polling
        return Map.of(
                "requestId", record.getId(),
                "status", record.getStatus(),
                "result", result,
                "processedAt", Instant.now().toString()
        );
    }

    /**
     * Register a new B2B partner. Returns partnerId and signing secret.
     */
    public Map<String, Object> registerPartner(String companyName, String email, String callbackUrl) {
        if (partnerRepository.existsByEmail(email)) {
            return Map.of("error", "Partner email already registered.");
        }

        WebhookPartner partner = new WebhookPartner();
        partner.setCompanyName(companyName);
        partner.setEmail(email);
        partner.setCallbackUrl(callbackUrl);
        partner.setSigningSecret(UUID.randomUUID().toString().replace("-", "") +
                                  UUID.randomUUID().toString().replace("-", ""));
        partner = partnerRepository.save(partner);

        return Map.of(
                "partnerId", partner.getId(),
                "companyName", partner.getCompanyName(),
                "signingSecret", partner.getSigningSecret(),
                "message", "B2B partner registered successfully. Store signingSecret securely — it will not be shown again."
        );
    }

    /**
     * Retrieve status of a previously submitted webhook request.
     */
    public Map<String, Object> getRequestStatus(String requestId) {
        return requestRepository.findById(requestId)
                .map(r -> Map.<String, Object>of(
                        "requestId", r.getId(),
                        "partnerId", r.getPartnerId(),
                        "requestType", r.getRequestType(),
                        "status", r.getStatus(),
                        "receivedAt", r.getReceivedAt().toString(),
                        "completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : "pending",
                        "result", r.getResult() != null ? r.getResult() : Map.of()
                ))
                .orElse(Map.of("error", "Request not found: " + requestId));
    }
}
