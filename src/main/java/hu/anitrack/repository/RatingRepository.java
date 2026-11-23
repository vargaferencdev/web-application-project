package hu.anitrack.repository;

import hu.anitrack.entity.Rating;
import hu.anitrack.repository.projection.RatingBreakdown;
import hu.anitrack.repository.projection.RatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndMediaId(Long userId, Long mediaId);

    /** Top rated: share of POSITIVE reactions, with a minimum number of votes. */
    @Query(value = """
            SELECT m.id AS "mediaId",
                   m.title AS "title",
                   m.media_type AS "mediaType",
                   COUNT(*) AS "votes",
                   CAST(ROUND(100.0 * COUNT(*) FILTER (WHERE r.reaction = 'POSITIVE') / COUNT(*), 1)
                        AS double precision) AS "positivePercent"
            FROM media m
            JOIN ratings r ON r.media_id = m.id
            GROUP BY m.id, m.title, m.media_type
            HAVING COUNT(*) >= :minVotes
            ORDER BY "positivePercent" DESC, "votes" DESC, m.title
            LIMIT :maxResults
            """, nativeQuery = true)
    List<RatingSummary> findTopRated(@Param("minVotes") int minVotes,
                                     @Param("maxResults") int maxResults);

    /** Reaction counts of one title (the service turns them into percentages). */
    @Query(value = """
            SELECT COUNT(*) AS "total",
                   COUNT(*) FILTER (WHERE reaction = 'POSITIVE') AS "positive",
                   COUNT(*) FILTER (WHERE reaction = 'NEUTRAL')  AS "neutral",
                   COUNT(*) FILTER (WHERE reaction = 'NEGATIVE') AS "negative"
            FROM ratings
            WHERE media_id = :mediaId
            """, nativeQuery = true)
    RatingBreakdown getBreakdown(@Param("mediaId") Long mediaId);
}
