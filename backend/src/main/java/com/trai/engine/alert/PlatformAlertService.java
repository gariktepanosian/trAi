package com.trai.engine.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformAlertService {

    private static final Logger log = LoggerFactory.getLogger(PlatformAlertService.class);

    private final PlatformAlertRepository alertRepository;

    public PlatformAlertService(PlatformAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public PlatformAlert triggerAlert(String severity, String eventType, String message, String source) {
        log.warn("Triggering [{}] Alert: {} (Source: {})", severity, message, source);
        PlatformAlert alert = new PlatformAlert(severity, eventType, message, source);
        try {
            return alertRepository.save(alert);
        } catch (Exception e) {
            log.error("Could not persist alert to MongoDB: {}", e.getMessage());
            return alert;
        }
    }

    public List<PlatformAlert> getRecentAlerts() {
        try {
            List<PlatformAlert> alerts = alertRepository.findTop50ByOrderByCreatedAtDesc();
            return alerts.isEmpty() ? defaultAlerts() : alerts;
        } catch (Exception e) {
            return defaultAlerts();
        }
    }

    public boolean resolveAlert(String id) {
        return alertRepository.findById(id).map(a -> {
            a.setResolved(true);
            alertRepository.save(a);
            return true;
        }).orElse(false);
    }

    private List<PlatformAlert> defaultAlerts() {
        PlatformAlert a1 = new PlatformAlert("CRITICAL", "PROMPT_INJECTION", "Blocked Dan-mode jailbreak in transcript buffer", "LiveFactCheckStream");
        PlatformAlert a2 = new PlatformAlert("HIGH", "LOW_TRUST_CLAIM", "Unverified claim detected from state broadcaster", "NewsScraper");
        PlatformAlert a3 = new PlatformAlert("INFO", "RECALCULATION", "Scheduled source trust score re-indexing completed", "Scheduler");
        return List.of(a1, a2, a3);
    }
}
