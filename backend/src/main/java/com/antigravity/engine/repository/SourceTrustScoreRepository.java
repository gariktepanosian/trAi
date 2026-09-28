package com.antigravity.engine.repository;

import com.antigravity.engine.domain.SourceTrustScore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceTrustScoreRepository extends MongoRepository<SourceTrustScore, String> {
    Optional<SourceTrustScore> findBySourceUrl(String sourceUrl);
}
