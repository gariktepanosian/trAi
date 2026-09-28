package com.trai.engine.alert;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAlertRepository extends MongoRepository<PlatformAlert, String> {
    List<PlatformAlert> findTop50ByOrderByCreatedAtDesc();
    List<PlatformAlert> findByResolvedFalseOrderByCreatedAtDesc();
}
