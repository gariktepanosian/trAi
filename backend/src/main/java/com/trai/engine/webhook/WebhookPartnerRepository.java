package com.trai.engine.webhook;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookPartnerRepository extends MongoRepository<WebhookPartner, String> {
    Optional<WebhookPartner> findByEmail(String email);
    boolean existsByEmail(String email);
}
