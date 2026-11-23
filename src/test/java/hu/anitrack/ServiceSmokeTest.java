package hu.anitrack;

import hu.anitrack.dto.AddFavoriteRequest;
import hu.anitrack.dto.AddToListRequest;
import hu.anitrack.dto.FavoriteResponse;
import hu.anitrack.dto.ListEntryResponse;
import hu.anitrack.dto.RatingRequest;
import hu.anitrack.dto.RatingSummaryResponse;
import hu.anitrack.dto.RegisterRequest;
import hu.anitrack.dto.UpdateListEntryRequest;
import hu.anitrack.dto.UserResponse;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.entity.enums.MediaType;
import hu.anitrack.entity.enums.Reaction;
import hu.anitrack.entity.enums.Role;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ForbiddenOperationException;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.UserRepository;
import hu.anitrack.service.FavoriteService;
import hu.anitrack.service.RatingService;
import hu.anitrack.service.UserMediaListService;
import hu.anitrack.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Walks through the user-facing services against the real database.
 * @Transactional makes every test roll back at the end, so the seed data stays untouched.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ServiceSmokeTest {

    @Autowired UserService userService;
    @Autowired UserMediaListService listService;
    @Autowired RatingService ratingService;
    @Autowired FavoriteService favoriteService;
    @Autowired MediaRepository mediaRepository;
    @Autowired UserRepository userRepository;

    @Test
    void registrationAndList() {
        UserResponse user = userService.register(
                new RegisterRequest("newbie", "Newbie@Example.com", "Secret123!"));
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.email()).isEqualTo("newbie@example.com");   // normalized

        // duplicate username is rejected
        assertThatThrownBy(() -> userService.register(
                new RegisterRequest("newbie", "other@example.com", "Secret123!")))
                .isInstanceOf(DuplicateResourceException.class);

        Media steins = media("Steins;Gate", MediaType.ANIME);   // 24 episodes in the seed data

        ListEntryResponse entry = listService.add(user.id(),
                new AddToListRequest(steins.getId(), ListStatus.CURRENT, 5));
        assertThat(entry.progress()).isEqualTo(5);
        assertThat(entry.total()).isEqualTo(24);
        assertThat(entry.startedAt()).isNotNull();

        // the same title cannot be added twice
        assertThatThrownBy(() -> listService.add(user.id(),
                new AddToListRequest(steins.getId(), ListStatus.PLANNING, 0)))
                .isInstanceOf(DuplicateResourceException.class);

        // progress above the total is rejected
        assertThatThrownBy(() -> listService.update(user.id(), entry.id(),
                new UpdateListEntryRequest(null, 25)))
                .isInstanceOf(BusinessRuleException.class);

        // completing sets progress to the total and the completion date
        ListEntryResponse done = listService.update(user.id(), entry.id(),
                new UpdateListEntryRequest(ListStatus.COMPLETED, null));
        assertThat(done.progress()).isEqualTo(24);
        assertThat(done.completedAt()).isNotNull();

        // ownership: another user cannot delete this entry
        Long testerId = userRepository.findByUsername("tester").orElseThrow().getId();
        assertThatThrownBy(() -> listService.delete(testerId, entry.id()))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(listService.getList(user.id(), null)).hasSize(1);
        listService.delete(user.id(), entry.id());
        assertThat(listService.getList(user.id(), null)).isEmpty();
    }

    @Test
    void ratingPercentages() {
        UserResponse user = userService.register(
                new RegisterRequest("rater", "rater@example.com", "Secret123!"));
        Media steins = media("Steins;Gate", MediaType.ANIME);

        // seed data: 2 POSITIVE ratings already exist for this title
        ratingService.rate(user.id(), steins.getId(), new RatingRequest(Reaction.POSITIVE));
        RatingSummaryResponse first = ratingService.getSummary(steins.getId());
        assertThat(first.total()).isEqualTo(3);
        assertThat(first.positivePercent()).isEqualTo(100.0);

        // rating again changes the existing rating, it does not add a new one
        ratingService.rate(user.id(), steins.getId(), new RatingRequest(Reaction.NEGATIVE));
        RatingSummaryResponse second = ratingService.getSummary(steins.getId());
        assertThat(second.total()).isEqualTo(3);
        assertThat(second.positive()).isEqualTo(2);
        assertThat(second.negative()).isEqualTo(1);
        assertThat(second.positivePercent()).isEqualTo(66.7);
    }

    @Test
    void favoritesLimit() {
        UserResponse user = userService.register(
                new RegisterRequest("fan", "fan@example.com", "Secret123!"));
        List<Media> all = mediaRepository.findAll();

        for (int i = 0; i < 6; i++) {
            favoriteService.add(user.id(), new AddFavoriteRequest(all.get(i).getId()));
        }

        // the 7th one is rejected
        assertThatThrownBy(() -> favoriteService.add(user.id(),
                new AddFavoriteRequest(all.get(6).getId())))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(favoriteService.getFavorites(user.id()))
                .extracting(FavoriteResponse::position)
                .containsExactly(1, 2, 3, 4, 5, 6);

        // after a removal the freed position is reused
        favoriteService.remove(user.id(), all.get(2).getId());
        FavoriteResponse readded = favoriteService.add(user.id(),
                new AddFavoriteRequest(all.get(6).getId()));
        assertThat(readded.position()).isEqualTo(3);
    }

    private Media media(String titlePart, MediaType type) {
        return mediaRepository
                .search(type.name(), titlePart, null, null, PageRequest.of(0, 1))
                .getContent().get(0);
    }
}
