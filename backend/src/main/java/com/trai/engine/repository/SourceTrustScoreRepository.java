package com.trai.engine.repository;

import com.trai.engine.domain.SourceTrustScore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SourceTrustScoreRepository extends MongoRepository<SourceTrustScore, String> {
    Optional<SourceTrustScore> findBySourceUrl(String sourceUrl);
}
