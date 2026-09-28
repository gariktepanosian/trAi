package com.trai.engine.webhook;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookRequestRepository extends MongoRepository<WebhookRequest, String> {
    // find all requests for a partner, ordered by receivedAt desc — use Pageable in service
}

