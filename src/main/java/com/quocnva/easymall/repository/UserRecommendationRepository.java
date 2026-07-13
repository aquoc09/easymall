package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.UserRecommendationEntity;
import com.quocnva.easymall.entity.UserRecommendationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendationEntity, UserRecommendationId> {

    @Query("SELECT ur FROM UserRecommendationEntity ur JOIN FETCH ur.product WHERE ur.id.userId = :userId ORDER BY ur.recommendationScore DESC LIMIT :limit")
    List<UserRecommendationEntity> findTopRecommendationsForUser(@Param("userId") Long userId, @Param("limit") int limit);
}
