package com.trai.engine.repository;

import com.trai.engine.domain.XUserRankScore;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for X (formerly Twitter) user authority rank scores.
 * Collection: x_user_rank_scores
 */
@Repository
public interface XUserRankScoreRepository extends MongoRepository<XUserRankScore, String> {

    /** Find a user's rank record by their X handle (without @). */
    Optional<XUserRankScore> findByUsername(String username);

    /** Find all users sorted by authority score descending (highest first). */
    List<XUserRankScore> findAllByOrderByAuthorityScoreDesc();

    /** Find users with authority score above a threshold — the "trusted" set. */
    List<XUserRankScore> findByAuthorityScoreGreaterThanEqualOrderByAuthorityScoreDesc(double minScore);

    /** Find verified users only (X Blue / legacy verified). */
    List<XUserRankScore> findByVerifiedTrueOrderByAuthorityScoreDesc();

    /** Find users in a specific tier. */
    List<XUserRankScore> findByAuthorityTierOrderByAuthorityScoreDesc(String tier);

    /** Find users that have NOT yet had real analytics collected (bootstrapped-only state). */
    @Query("{ 'analyticsBootstrapped': false }")
    List<XUserRankScore> findNonBootstrapped();

    /** Find users whose total posts analyzed is above a threshold (data-rich profiles). */
    List<XUserRankScore> findByTotalPostsAnalyzedGreaterThan(int minPosts);
}
