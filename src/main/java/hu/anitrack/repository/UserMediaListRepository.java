package hu.anitrack.repository;

import hu.anitrack.entity.UserMediaList;
import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.repository.projection.GenreShare;
import hu.anitrack.repository.projection.PopularMedia;
import hu.anitrack.repository.projection.UserStats;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMediaListRepository extends JpaRepository<UserMediaList, Long> {

    // @EntityGraph loads the media together with the list rows (one query),
    // so it can be used after the transaction ended (open-in-view is off)
    @EntityGraph(attributePaths = "media")
    List<UserMediaList> findByUserId(Long userId);

    @EntityGraph(attributePaths = "media")
    List<UserMediaList> findByUserIdAndStatus(Long userId, ListStatus status);

    Optional<UserMediaList> findByUserIdAndMediaId(Long userId, Long mediaId);

    boolean existsByUserIdAndMediaId(Long userId, Long mediaId);

    /** All-time popular: on how many users' lists a title is. */
    @Query(value = """
            SELECT m.id AS "mediaId",
                   m.title AS "title",
                   m.media_type AS "mediaType",
                   COUNT(uml.id) AS "listCount",
                   m.cover_image_url AS "coverImageUrl"
            FROM media m
            JOIN user_media_list uml ON uml.media_id = m.id
            GROUP BY m.id, m.title, m.media_type, m.cover_image_url
            ORDER BY "listCount" DESC, m.title
            LIMIT :maxResults
            """, nativeQuery = true)
    List<PopularMedia> findMostPopular(@Param("maxResults") int maxResults);

    /** Statistics of one user, computed from their list (nothing is stored). */
    @Query(value = """
            SELECT COUNT(*) AS "totalTitles",
                   COUNT(*) FILTER (WHERE m.media_type = 'ANIME') AS "animeCount",
                   COUNT(*) FILTER (WHERE m.media_type = 'MANGA') AS "mangaCount",
                   COALESCE(SUM(uml.progress) FILTER (WHERE m.media_type = 'ANIME'), 0) AS "episodesWatched",
                   COALESCE(SUM(uml.progress) FILTER (WHERE m.media_type = 'MANGA'), 0) AS "chaptersRead",
                   COALESCE(SUM(uml.progress * m.duration_minutes)
                            FILTER (WHERE m.media_type = 'ANIME'), 0) AS "minutesWatched"
            FROM user_media_list uml
            JOIN media m ON m.id = uml.media_id
            WHERE uml.user_id = :userId
            """, nativeQuery = true)
    UserStats getUserStats(@Param("userId") Long userId);

    /** The genres a user has the most titles of on their list (for the statistics page). */
    @Query(value = """
            SELECT g.name AS "name",
                   COUNT(*) AS "titleCount"
            FROM user_media_list uml
            JOIN media_genres mg ON mg.media_id = uml.media_id
            JOIN genres g ON g.id = mg.genre_id
            WHERE uml.user_id = :userId
            GROUP BY g.name
            ORDER BY "titleCount" DESC, g.name
            LIMIT :maxResults
            """, nativeQuery = true)
    List<GenreShare> findTopGenres(@Param("userId") Long userId, @Param("maxResults") int maxResults);
}
