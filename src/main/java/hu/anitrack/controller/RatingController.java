package hu.anitrack.controller;

import hu.anitrack.dto.RatingRequest;
import hu.anitrack.dto.RatingResponse;
import hu.anitrack.security.AuthenticatedUser;
import hu.anitrack.service.RatingService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user's own ratings. The public summary (percentages) is part of the media detail.
 * PUT is idempotent: it creates the rating or replaces the existing one.
 */
@RestController
@RequestMapping("/api/me/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/{mediaId}")
    public RatingResponse getMine(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long mediaId) {
        return ratingService.getMyRating(user.id(), mediaId);
    }

    @PutMapping("/{mediaId}")
    public RatingResponse rate(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long mediaId,
            @Valid @RequestBody RatingRequest request) {
        return ratingService.rate(user.id(), mediaId, request);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long mediaId) {
        ratingService.remove(user.id(), mediaId);
    }
}