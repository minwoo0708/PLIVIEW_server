package com.musicreview.server.review.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // ===== 피드 =====

    /** 최신 리뷰 피드. type이 null이면 전체 */
    @EntityGraph(attributePaths = "user")
    @Query(value = "select r from Review r where (:type is null or r.type = :type)",
            countQuery = "select count(r) from Review r where (:type is null or r.type = :type)")
    Page<Review> findFeed(@Param("type") ItemType type, Pageable pageable);

    /** 인기 항목: since 이후 작성된 리뷰 수 내림차순, 같으면 평균 별점 내림차순 */
    @Query("""
            select new com.musicreview.server.review.domain.PopularItemRow(
                r.type, r.spotifyId, count(r), avg(r.rating))
            from Review r
            where r.createdAt >= :since and (:type is null or r.type = :type)
            group by r.type, r.spotifyId
            order by count(r) desc, avg(r.rating) desc, r.spotifyId asc
            """)
    List<PopularItemRow> findPopularItems(@Param("type") ItemType type, @Param("since") LocalDateTime since,
                                          Pageable pageable);

    /** 항목의 가장 최근 리뷰 (표시용 이름·이미지를 가져올 때) */
    Optional<Review> findFirstByTypeAndSpotifyIdOrderByCreatedAtDescIdDesc(ItemType type, String spotifyId);

    // ===== 유저 리뷰·통계 =====

    /** 유저의 리뷰. type, minRating이 null이면 조건 없음 */
    @EntityGraph(attributePaths = "user")
    @Query(value = """
            select r from Review r
            where r.user.id = :userId
              and (:type is null or r.type = :type)
              and (:minRating is null or r.rating >= :minRating)
            """,
            countQuery = """
            select count(r) from Review r
            where r.user.id = :userId
              and (:type is null or r.type = :type)
              and (:minRating is null or r.rating >= :minRating)
            """)
    Page<Review> findUserReviews(@Param("userId") Long userId, @Param("type") ItemType type,
                                 @Param("minRating") BigDecimal minRating, Pageable pageable);

    /** 유저 전체 리뷰 수와 평균 별점: [count, avg] */
    @Query("select count(r), avg(r.rating) from Review r where r.user.id = :userId")
    List<Object[]> findUserTotals(@Param("userId") Long userId);

    @Query("select r.rating, count(r) from Review r where r.user.id = :userId group by r.rating")
    List<Object[]> countUserReviewsByRating(@Param("userId") Long userId);

    /** [type, count] */
    @Query("select r.type, count(r) from Review r where r.user.id = :userId group by r.type")
    List<Object[]> countUserReviewsByType(@Param("userId") Long userId);

    // ===== 큐레이터 랭킹 =====

    /** 플리 주인별 집계. 받은 리뷰가 minReviews 이상인 주인만, 평균 총점 내림차순 */
    @Query("""
            select new com.musicreview.server.review.domain.CuratorRow(
                r.ownerSpotifyId, count(distinct r.spotifyId), count(r),
                avg(r.rating), avg(r.conceptScore), avg(r.flowScore), avg(r.discoveryScore))
            from Review r
            where r.type = com.musicreview.server.review.domain.ItemType.PLAYLIST
            group by r.ownerSpotifyId
            having count(r) >= :minReviews
            order by avg(r.rating) desc, count(r) desc, r.ownerSpotifyId asc
            """)
    List<CuratorRow> findCuratorRanking(@Param("minReviews") long minReviews, Pageable pageable);

    /** 주인의 가장 최근 플리 리뷰 (주인 표시 이름용) */
    Optional<Review> findFirstByTypeAndOwnerSpotifyIdOrderByCreatedAtDescIdDesc(ItemType type, String ownerSpotifyId);
}
