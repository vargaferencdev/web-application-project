package hu.anitrack.repository;

import hu.anitrack.entity.Genre;
import hu.anitrack.repository.projection.GenreCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByName(String name);

    boolean existsByName(String name);

    /** How many titles each genre has. LEFT JOIN keeps genres without any title (count 0). */
    @Query(value = """
            SELECT g.id AS "genreId",
                   g.name AS "name",
                   COUNT(mg.media_id) AS "mediaCount"
            FROM genres g
            LEFT JOIN media_genres mg ON mg.genre_id = g.id
            GROUP BY g.id, g.name
            ORDER BY g.name
            """, nativeQuery = true)
    List<GenreCount> countMediaPerGenre();
}
