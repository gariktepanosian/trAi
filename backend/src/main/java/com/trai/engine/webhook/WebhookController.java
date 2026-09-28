package com.trai.engine.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * B2B Webhook REST API.
 *
 * Endpoints:
 *   POST /api/v1/webhook/register            — register a new B2B partner (requires BUSINESS role)
 *   POST /api/v1/webhook/ingest              — main entry point for partner systems (HMAC-signed, public)
 *   GET  /api/v1/webhook/status/{requestId}  — poll result of an async request (requires BUSINESS role)
 */
@RestController
@RequestMapping("/api/v1/webhook")
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Register a new B2B partner.
     * Body: { "companyName": "...", "email": "...", "callbackUrl": "..." }
     * Returns: partnerId + signingSecret (show once only).
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerPartner(@RequestBody Map<String, String> body) {
        String companyName = body.get("companyName");
        String email = body.get("email");
        String callbackUrl = body.getOrDefault("callbackUrl", "");

        if (companyName == null || email == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "companyName and email are required."));
        }

        Map<String, Object> result = webhookService.registerPartner(companyName, email, callbackUrl);
        if (result.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Main B2B ingest endpoint.
     *
     * Headers:
     *   X-TrAI-Partner-ID: <partnerId>
     *   X-TrAI-Signature:  <HMAC-SHA256 hex of request body>
     *
     * Body:
     * {
     *   "requestType": "NEWS_VERIFY | AI_AUDIT | LIVE_FACT_CHECK",
     *   "payload": { ... request-type-specific fields ... }
     * }
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(
            @RequestHeader(value = "X-TrAI-Partner-ID", required = false) String partnerId,
            @RequestHeader(value = "X-TrAI-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        // If partner credentials provided, verify signature
        if (partnerId != null && signature != null) {
            boolean valid = webhookService.verifySignature(partnerId, rawBody, signature);
            if (!valid) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Webhook signature verification failed."));
            }
        }

        // Parse raw body manually to keep routing clean
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = mapper.readValue(rawBody, Map.class);
            String requestType = (String) body.getOrDefault("requestType", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) body.getOrDefault("payload", Map.of());

            if (requestType.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "requestType is required.",
                                "supported", "NEWS_VERIFY, AI_AUDIT, LIVE_FACT_CHECK"));
            }

            String effectivePartnerId = partnerId != null ? partnerId : "anonymous";
            Map<String, Object> result = webhookService.processIngest(effectivePartnerId, requestType, payload);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid JSON payload.", "detail", e.getMessage()));
        }
    }

    /**
     * Poll the result of a previously submitted webhook request.
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String requestId) {
        Map<String, Object> result = webhookService.getRequestStatus(requestId);
        if (result.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
