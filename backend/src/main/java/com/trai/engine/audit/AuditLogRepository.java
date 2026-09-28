package com.trai.engine.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findByActorId(String actorId, Pageable pageable);
    Page<AuditLog> findByActionType(String actionType, Pageable pageable);
    List<AuditLog> findByCreatedAtBetween(Instant from, Instant to);
    long countByBlockedTrue();
    long countByActionType(String actionType);
}
