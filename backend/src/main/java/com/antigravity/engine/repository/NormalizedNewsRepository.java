package com.antigravity.engine.repository;

import com.antigravity.engine.domain.NormalizedNews;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormalizedNewsRepository extends MongoRepository<NormalizedNews, String> {
    List<NormalizedNews> findBySearchTagsContaining(String tag);

    List<NormalizedNews> findByEventId(String eventId);
}
