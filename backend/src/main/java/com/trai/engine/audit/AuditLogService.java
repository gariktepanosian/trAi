package com.trai.engine.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Audit log service — creates log entries and generates summary reports.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Record an audit entry.
     */
    public AuditLog record(String actorId, String actionType, String inputSummary,
                           String verdict, Integer trustScore, List<String> flags,
                           boolean blocked, Long processingTimeMs) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId != null ? actorId : "anonymous");
        log.setActionType(actionType);
        log.setInputSummary(
                inputSummary != null ? inputSummary.substring(0, Math.min(200, inputSummary.length())) : "");
        log.setVerdict(verdict);
        log.setTrustScore(trustScore);
        log.setFlags(flags);
        log.setBlocked(blocked);
        log.setProcessingTimeMs(processingTimeMs);
        return repository.save(log);
    }

    /**
     * Paginated audit log retrieval.
     */
    public Page<AuditLog> getRecentLogs(int page, int size) {
        return repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Logs for a specific actor.
     */
    public Page<AuditLog> getLogsByActor(String actorId, int page, int size) {
        return repository.findByActorId(actorId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Generate a summary report for a time window.
     */
    public Map<String, Object> generateSummaryReport(Instant from, Instant to) {
        List<AuditLog> logs = repository.findByCreatedAtBetween(from, to);

        long total = logs.size();
        long blocked = logs.stream().filter(AuditLog::isBlocked).count();
        long liveFactChecks = logs.stream().filter(l -> "LIVE_FACT_CHECK".equals(l.getActionType())).count();
        long aiAudits = logs.stream().filter(l -> "AI_AUDIT".equals(l.getActionType())).count();
        long newsVerify = logs.stream().filter(l -> "NEWS_VERIFY".equals(l.getActionType())).count();
        long webhooks = logs.stream().filter(l -> "WEBHOOK".equals(l.getActionType())).count();

        double avgTrustScore = logs.stream()
                .filter(l -> l.getTrustScore() != null)
                .mapToInt(AuditLog::getTrustScore)
                .average()
                .orElse(0.0);

        long injectionAttempts = logs.stream()
                .filter(l -> l.getFlags() != null && l.getFlags().contains("PROMPT_INJECTION_DETECTED"))
                .count();

        return Map.of(
                "reportPeriod", Map.of("from", from.toString(), "to", to.toString()),
                "totalRequests", total,
                "blockedRequests", blocked,
                "blockRate", total > 0 ? String.format("%.1f%%", (blocked * 100.0) / total) : "0%",
                "averageTrustScore", String.format("%.1f", avgTrustScore),
                "injectionAttempts", injectionAttempts,
                "byActionType", Map.of(
                        "LIVE_FACT_CHECK", liveFactChecks,
                        "AI_AUDIT", aiAudits,
                        "NEWS_VERIFY", newsVerify,
                        "WEBHOOK", webhooks
                ),
                "generatedAt", Instant.now().toString()
        );
    }
}
