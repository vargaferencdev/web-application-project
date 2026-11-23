package hu.anitrack;

import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.PageResponse;
import hu.anitrack.dto.MediaSummaryResponse;
import hu.anitrack.dto.UserStatsResponse;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.repository.UserRepository;
import hu.anitrack.service.CatalogService;
import hu.anitrack.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the catalog and the statistics against the known seed data (V2__seed.sql).
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class CatalogStatsSmokeTest {

    @Autowired CatalogService catalogService;
    @Autowired StatisticsService statisticsService;
    @Autowired UserRepository userRepository;

    @Test
    void searchFilters() {
        // title, case-insensitive: Death Note anime + manga
        assertThat(catalogService.search(null, "death", null, null, 0, 10).totalElements()).isEqualTo(2);

        // genre filter: Steins;Gate, Death Note (anime and manga), Monster
        Long thrillerId = genreId("Thriller");
        assertThat(catalogService.search(null, null, thrillerId, null, 0, 10).totalElements()).isEqualTo(4);

        // year filter: Fullmetal Alchemist (manga) and Spirited Away started in 2001
        assertThat(catalogService.search(null, null, null, 2001, 0, 10).totalElements()).isEqualTo(2);

        // type filter, lower case is accepted: 6 manga in the seed data
        assertThat(catalogService.search("manga", null, null, null, 0, 20).totalElements()).isEqualTo(6);

        // paging: 16 titles, 5 per page -> 4 pages
        PageResponse<MediaSummaryResponse> page = catalogService.search(null, null, null, null, 0, 5);
        assertThat(page.content()).hasSize(5);
        assertThat(page.totalElements()).isEqualTo(16);
        assertThat(page.totalPages()).isEqualTo(4);

        // unknown type is a clear error
        assertThatThrownBy(() -> catalogService.search("book", null, null, null, 0, 10))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void detailPage() {
        Long id = catalogService.search("ANIME", "steins", null, null, 0, 5).content().get(0).id();

        MediaDetailResponse detail = catalogService.getById(id);
        assertThat(detail.genres()).containsExactly("Drama", "Sci-Fi", "Thriller");
        assertThat(detail.tags()).containsExactly("Time Travel");
        assertThat(detail.ratings().total()).isEqualTo(2);
        assertThat(detail.ratings().positivePercent()).isEqualTo(100.0);

        assertThatThrownBy(() -> catalogService.getById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rankings() {
        assertThat(catalogService.topRated(2, 10).get(0).title())
                .isEqualTo("Fullmetal Alchemist: Brotherhood");

        var mostPopular = catalogService.popular(3).get(0);
        assertThat(mostPopular.title()).isEqualTo("Death Note");
        assertThat(mostPopular.listCount()).isEqualTo(3);

        // LEFT JOIN + COUNT: Mystery is on Death Note (anime, manga) and Monster
        var mystery = catalogService.genres().stream()
                .filter(g -> g.name().equals("Mystery")).findFirst().orElseThrow();
        assertThat(mystery.mediaCount()).isEqualTo(3);
    }

    @Test
    void statisticsOfTester() {
        Long testerId = userRepository.findByUsername("tester").orElseThrow().getId();

        UserStatsResponse stats = statisticsService.getStats(testerId);
        assertThat(stats.totalTitles()).isEqualTo(6);
        assertThat(stats.animeCount()).isEqualTo(4);
        assertThat(stats.mangaCount()).isEqualTo(2);
        assertThat(stats.episodesWatched()).isEqualTo(102);
        assertThat(stats.chaptersRead()).isEqualTo(90);
        assertThat(stats.minutesWatched()).isEqualTo(2448);
        assertThat(stats.estimatedDaysWatched()).isEqualTo(1.7);
    }

    private Long genreId(String name) {
        return catalogService.genres().stream()
                .filter(g -> g.name().equals(name))
                .findFirst().orElseThrow().id();
    }
}