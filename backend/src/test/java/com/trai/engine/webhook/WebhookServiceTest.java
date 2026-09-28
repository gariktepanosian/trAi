package com.trai.engine.webhook;

import com.trai.engine.service.TrustVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService Unit Tests")
class WebhookServiceTest {

    @Mock
    private TrustVerificationService trustService;

    @Mock
    private WebhookRequestRepository requestRepository;

    @Mock
    private WebhookPartnerRepository partnerRepository;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(trustService, requestRepository, partnerRepository);
    }

    @Test
    @DisplayName("Should register partner and generate signing secret")
    void testRegisterPartner() {
        when(partnerRepository.existsByEmail("desk@reuters.com")).thenReturn(false);
        when(partnerRepository.save(any(WebhookPartner.class))).thenAnswer(inv -> {
            WebhookPartner p = inv.getArgument(0);
            p.setId("partner-123");
            return p;
        });

        Map<String, Object> result = webhookService.registerPartner("Reuters News Desk", "desk@reuters.com", "https://reuters.com/callback");

        assertNotNull(result);
        assertEquals("partner-123", result.get("partnerId"));
        assertEquals("Reuters News Desk", result.get("companyName"));
        assertNotNull(result.get("signingSecret"));
        assertTrue(((String) result.get("signingSecret")).length() >= 32);
    }

    @Test
    @DisplayName("Should verify HMAC signature correctly")
    void testVerifySignature() throws Exception {
        String partnerId = "p-1";
        String secret = "test-secret-key-1234567890123456";
        String body = "{\"event\":\"breaking_news\",\"text\":\"Test event\"}";

        WebhookPartner partner = new WebhookPartner();
        partner.setId(partnerId);
        partner.setSigningSecret(secret);

        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        // Compute real HMAC
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String validSignature = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));

        assertTrue(webhookService.verifySignature(partnerId, body, validSignature));
        assertFalse(webhookService.verifySignature(partnerId, body, "invalid-signature"));
        assertFalse(webhookService.verifySignature("unknown-partner", body, validSignature));
    }

    @Test
    @DisplayName("Should route NEWS_VERIFY request type correctly")
    void testProcessIngestNewsVerify() {
        String partnerId = "p-1";
        Map<String, Object> payload = Map.of(
                "text", "Breaking economic report",
                "sourceName", "Bloomberg"
        );

        when(requestRepository.save(any(WebhookRequest.class))).thenAnswer(inv -> {
            WebhookRequest req = inv.getArgument(0);
            if (req.getId() == null) req.setId("req-456");
            return req;
        });

        when(trustService.verifyNewsArticle(any())).thenReturn(Map.of("status", "SUCCESS", "analysis", "verified"));

        Map<String, Object> response = webhookService.processIngest(partnerId, "NEWS_VERIFY", payload);

        assertNotNull(response);
        assertEquals("req-456", response.get("requestId"));
        assertEquals("COMPLETED", response.get("status"));
        assertEquals("SUCCESS", ((Map<?, ?>) response.get("result")).get("status"));
        verify(trustService, times(1)).verifyNewsArticle(any());
    }
}
