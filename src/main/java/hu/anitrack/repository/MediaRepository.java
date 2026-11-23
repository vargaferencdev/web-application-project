package hu.anitrack.repository;

import hu.anitrack.entity.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * Catalog search. Every filter is optional (null = no filter).
     * The CAST(... AS type) is needed because PostgreSQL cannot infer the type
     * of a NULL parameter in "param IS NULL" (and the :: cast syntax would
     * clash with named parameters).
     */
    @Query(value = """
            SELECT m.*
            FROM media m
            WHERE (CAST(:type AS text) IS NULL OR m.media_type = CAST(:type AS text))
              AND (CAST(:title AS text) IS NULL
                   OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS text), '%')))
              AND (CAST(:genreId AS bigint) IS NULL
                   OR EXISTS (SELECT 1
                              FROM media_genres mg
                              WHERE mg.media_id = m.id
                                AND mg.genre_id = CAST(:genreId AS bigint)))
              AND (CAST(:year AS integer) IS NULL
                   OR EXTRACT(YEAR FROM m.start_date) = CAST(:year AS integer))
            ORDER BY m.title
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM media m
            WHERE (CAST(:type AS text) IS NULL OR m.media_type = CAST(:type AS text))
              AND (CAST(:title AS text) IS NULL
                   OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS text), '%')))
              AND (CAST(:genreId AS bigint) IS NULL
                   OR EXISTS (SELECT 1
                              FROM media_genres mg
                              WHERE mg.media_id = m.id
                                AND mg.genre_id = CAST(:genreId AS bigint)))
              AND (CAST(:year AS integer) IS NULL
                   OR EXTRACT(YEAR FROM m.start_date) = CAST(:year AS integer))
            """,
            nativeQuery = true)
    Page<Media> search(@Param("type") String type,
                       @Param("title") String title,
                       @Param("genreId") Long genreId,
                       @Param("year") Integer year,
                       Pageable pageable);
}
