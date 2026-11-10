package com.musicreview.server.review.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndTypeAndSpotifyId(Long userId, ItemType type, String spotifyId);

    @EntityGraph(attributePaths = "user")
    Optional<Review> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Review> findByUserIdAndTypeAndSpotifyId(Long userId, ItemType type, String spotifyId);

    @EntityGraph(attributePaths = "user")
    Page<Review> findByTypeAndSpotifyId(ItemType type, String spotifyId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Review> findByUserIdAndTypeAndSpotifyIdIn(Long userId, ItemType type, Collection<String> spotifyIds);

    List<Review> findByUserId(Long userId);

    /** 항목 요약: 리뷰 수와 평균들 (리뷰가 없으면 평균은 null) */
    @Query("""
            select new com.musicreview.server.review.domain.ItemStats(
                count(r), avg(r.rating), avg(r.conceptScore), avg(r.flowScore), avg(r.discoveryScore))
            from Review r
            where r.type = :type and r.spotifyId = :spotifyId
            """)
    ItemStats findItemStats(@Param("type") ItemType type, @Param("spotifyId") String spotifyId);

    /** 별점 분포: [rating, count] */
    @Query("""
            select r.rating, count(r)
            from Review r
            where r.type = :type and r.spotifyId = :spotifyId
            group by r.rating
            """)
    List<Object[]> countByRating(@Param("type") ItemType type, @Param("spotifyId") String spotifyId);
}
