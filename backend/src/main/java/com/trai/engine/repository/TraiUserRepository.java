package com.trai.engine.repository;

import com.trai.engine.domain.TraiUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TraiUserRepository extends MongoRepository<TraiUser, String> {
    Optional<TraiUser> findByUsername(String username);
    Optional<TraiUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
