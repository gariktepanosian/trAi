package com.trai.engine.repository;

import com.trai.engine.domain.NormalizedNews;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormalizedNewsRepository extends MongoRepository<NormalizedNews, String> {
    List<NormalizedNews> findBySearchTagsContaining(String tag);
    List<NormalizedNews> findByEventId(String eventId);
    List<NormalizedNews> findTop20ByOrderByCreatedAtDesc();
    List<NormalizedNews> findBySourceNameContainingIgnoreCaseOrderByCreatedAtDesc(String sourceName);
}
