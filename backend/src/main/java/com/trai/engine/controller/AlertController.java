package com.trai.engine.controller;

import com.trai.engine.alert.PlatformAlert;
import com.trai.engine.alert.PlatformAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final PlatformAlertService alertService;

    public AlertController(PlatformAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<PlatformAlert>> getAlerts() {
        return ResponseEntity.ok(alertService.getRecentAlerts());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable String id) {
        boolean resolved = alertService.resolveAlert(id);
        return ResponseEntity.ok(Map.of("id", id, "resolved", resolved));
    }
}
