package com.trai.engine.country;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * CountryController — REST endpoints for country subscription management.
 *
 * Mobile app calls these endpoints when user selects/changes their country.
 */
@RestController
@RequestMapping("/api/v1/country")
public class CountryController {

    private final CountryMonitoringService monitoringService;

    public CountryController(CountryMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * POST /api/v1/country/subscribe
     * Body: { "country": "Armenia", "fcmToken": "device-fcm-token" }
     *
     * Called when user selects their country in the app.
     * Also called when the app receives a new FCM registration token.
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody SubscribeRequest request) {
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Country is required"));
        }
        if (request.getFcmToken() == null || request.getFcmToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "FCM token is required"));
        }

        monitoringService.subscribeUserToCountry(request.getCountry(), request.getFcmToken());

        return ResponseEntity.ok(Map.of(
                "status", "SUBSCRIBED",
                "country", request.getCountry(),
                "message", "You will receive push notifications for critical events in " + request.getCountry(),
                "subscriberCount", monitoringService.getSubscriberCount(request.getCountry())
        ));
    }

    /**
     * POST /api/v1/country/unsubscribe
     * Body: { "country": "Armenia", "fcmToken": "device-fcm-token" }
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody SubscribeRequest request) {
        monitoringService.unsubscribeUserFromCountry(request.getCountry(), request.getFcmToken());
        return ResponseEntity.ok(Map.of(
                "status", "UNSUBSCRIBED",
                "country", request.getCountry()
        ));
    }

    /**
     * GET /api/v1/country/monitored
     * Returns the list of all currently monitored countries.
     */
    @GetMapping("/monitored")
    public ResponseEntity<Map<String, Object>> getMonitoredCountries() {
        Set<String> countries = monitoringService.getMonitoredCountries();
        return ResponseEntity.ok(Map.of(
                "countries", countries,
                "total", countries.size()
        ));
    }

    /**
     * POST /api/v1/country/trigger-check
     * Body: { "country": "Armenia" }
     * Manually trigger an immediate monitoring check for a country (admin use).
     */
    @PostMapping("/trigger-check")
    public ResponseEntity<Map<String, Object>> triggerManualCheck(@RequestBody Map<String, String> body) {
        String country = body.get("country");
        if (country == null || country.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Country is required"));
        }

        CountryMonitorResult result = monitoringService.monitorCountry(country);
        return ResponseEntity.ok(Map.of(
                "country", result.getCountry(),
                "tweetsAnalyzed", result.getTweetsAnalyzed(),
                "verdict", result.getVerdict(),
                "trustScore", result.getTrustScore(),
                "criticalAlert", result.isCriticalAlert(),
                "criticalLevel", result.getCriticalLevel(),
                "timestamp", result.getTimestamp()
        ));
    }

    // ─── Request DTO ──────────────────────────────────────────────────────────

    public static class SubscribeRequest {
        private String country;
        private String fcmToken;

        public String getCountry()   { return country; }
        public String getFcmToken()  { return fcmToken; }
        public void setCountry(String v)   { this.country = v; }
        public void setFcmToken(String v)  { this.fcmToken = v; }
    }
}
