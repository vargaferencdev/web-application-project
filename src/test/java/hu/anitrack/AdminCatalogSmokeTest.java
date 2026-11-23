package hu.anitrack;

import hu.anitrack.dto.AddToListRequest;
import hu.anitrack.dto.GenreResponse;
import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.MediaRequest;
import hu.anitrack.dto.NameRequest;
import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.entity.enums.MediaFormat;
import hu.anitrack.entity.enums.MediaStatus;
import hu.anitrack.entity.enums.MediaType;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.repository.UserRepository;
import hu.anitrack.service.AdminCatalogService;
import hu.anitrack.service.CatalogService;
import hu.anitrack.service.UserMediaListService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rolls back at the end, so the catalog in the database stays untouched. */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AdminCatalogSmokeTest {

    @Autowired AdminCatalogService adminService;
    @Autowired CatalogService catalogService;
    @Autowired UserMediaListService listService;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager entityManager;

    @Test
    void createTitleWithGenres() {
        Long action = genreId("Action");
        Long comedy = genreId("Comedy");

        MediaDetailResponse created = adminService.createMedia(
                anime("Zzz Admin Test Anime", MediaStatus.FINISHED, 12, null, Set.of(action, comedy)));

        assertThat(created.id()).isNotNull();
        assertThat(created.genres()).containsExactly("Action", "Comedy");
        assertThat(created.ratings().total()).isZero();
        assertThat(catalogService.search("ANIME", "zzz admin test", null, null, 0, 10).totalElements())
                .isEqualTo(1);
    }

    @Test
    void businessRulesAreChecked() {
        // an anime cannot have chapters
        assertThatThrownBy(() -> adminService.createMedia(
                anime("Zzz Bad", MediaStatus.FINISHED, 12, 10, Set.of())))
                .isInstanceOf(BusinessRuleException.class);

        // RELEASING is a manga status
        assertThatThrownBy(() -> adminService.createMedia(
                anime("Zzz Bad", MediaStatus.RELEASING, 12, null, Set.of())))
                .isInstanceOf(BusinessRuleException.class);

        // MANGA is not an anime format
        assertThatThrownBy(() -> adminService.createMedia(new MediaRequest(
                "Zzz Bad", null, MediaType.ANIME, MediaFormat.MANGA, MediaStatus.FINISHED,
                null, null, 12, null, null, 24, null, Set.of(), Set.of())))
                .isInstanceOf(BusinessRuleException.class);

        // a manga cannot have episodes
        assertThatThrownBy(() -> adminService.createMedia(new MediaRequest(
                "Zzz Bad", null, MediaType.MANGA, MediaFormat.MANGA, MediaStatus.FINISHED,
                null, null, 12, null, null, null, null, Set.of(), Set.of())))
                .isInstanceOf(BusinessRuleException.class);

        // the end cannot be before the start
        assertThatThrownBy(() -> adminService.createMedia(new MediaRequest(
                "Zzz Bad", null, MediaType.ANIME, MediaFormat.TV, MediaStatus.FINISHED,
                LocalDate.of(2024, 5, 1), LocalDate.of(2024, 1, 1), 12, null, null, 24, null,
                Set.of(), Set.of())))
                .isInstanceOf(BusinessRuleException.class);

        // unknown genre
        assertThatThrownBy(() -> adminService.createMedia(
                anime("Zzz Bad", MediaStatus.FINISHED, 12, null, Set.of(999_999L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAndDelete() {
        MediaDetailResponse created = adminService.createMedia(
                anime("Zzz Admin Update", MediaStatus.FINISHED, 12, null, Set.of(genreId("Action"))));

        MediaDetailResponse updated = adminService.updateMedia(created.id(),
                anime("Zzz Admin Renamed", MediaStatus.AIRING, null, null, Set.of(genreId("Comedy"))));
        assertThat(updated.title()).isEqualTo("Zzz Admin Renamed");
        assertThat(updated.status()).isEqualTo(MediaStatus.AIRING);
        assertThat(updated.genres()).containsExactly("Comedy");
        assertThat(catalogService.getById(created.id()).title()).isEqualTo("Zzz Admin Renamed");

        adminService.deleteMedia(created.id());
        assertThatThrownBy(() -> catalogService.getById(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> adminService.deleteMedia(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletingATitleRemovesItFromUserLists() {
        Long testerId = userRepository.findByUsername("tester").orElseThrow().getId();
        int before = listService.getList(testerId, null).size();

        MediaDetailResponse created = adminService.createMedia(
                anime("Zzz Admin Cascade", MediaStatus.FINISHED, 12, null, Set.of()));
        listService.add(testerId, new AddToListRequest(created.id(), ListStatus.PLANNING, 0));
        assertThat(listService.getList(testerId, null)).hasSize(before + 1);

        // write everything to the database and forget the cached entities, so the
        // list entry is no longer held in memory pointing at the title we delete
        entityManager.flush();
        entityManager.clear();

        adminService.deleteMedia(created.id());
        entityManager.flush();
        entityManager.clear();   // forget the cached entities, read the real database state

        // ON DELETE CASCADE removed the list entry together with the title
        assertThat(listService.getList(testerId, null)).hasSize(before);
    }

    @Test
    void genresAndTags() {
        GenreResponse created = adminService.createGenre(new NameRequest("Zzz Test Genre"));
        assertThat(created.mediaCount()).isZero();

        assertThatThrownBy(() -> adminService.createGenre(new NameRequest("Zzz Test Genre")))
                .isInstanceOf(DuplicateResourceException.class);
        // renaming to an existing name of another genre is rejected as well
        assertThatThrownBy(() -> adminService.updateGenre(created.id(), new NameRequest("Action")))
                .isInstanceOf(DuplicateResourceException.class);

        GenreResponse renamed = adminService.updateGenre(created.id(), new NameRequest("Zzz Renamed Genre"));
        assertThat(renamed.name()).isEqualTo("Zzz Renamed Genre");

        adminService.deleteGenre(created.id());
        assertThatThrownBy(() -> adminService.deleteGenre(created.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        var tag = adminService.createTag(new NameRequest("Zzz Test Tag"));
        assertThatThrownBy(() -> adminService.createTag(new NameRequest("Zzz Test Tag")))
                .isInstanceOf(DuplicateResourceException.class);
        adminService.deleteTag(tag.id());
    }

    private MediaRequest anime(String title, MediaStatus status, Integer episodes, Integer chapters,
                               Set<Long> genreIds) {
        return new MediaRequest(title, "Test description", MediaType.ANIME, MediaFormat.TV, status,
                LocalDate.of(2024, 1, 1), null, episodes, chapters, null, 24, null, genreIds, Set.of());
    }

    private Long genreId(String name) {
        return catalogService.genres().stream()
                .filter(g -> g.name().equals(name))
                .findFirst().orElseThrow().id();
    }
}