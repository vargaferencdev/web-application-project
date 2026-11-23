package hu.anitrack.repository;

import hu.anitrack.entity.Favorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = "media")
    List<Favorite> findByUserIdOrderByPositionAsc(Long userId);

    Optional<Favorite> findByUserIdAndMediaId(Long userId, Long mediaId);

    long countByUserId(Long userId);
}
