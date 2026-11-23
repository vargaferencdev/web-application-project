package hu.anitrack.controller;

import hu.anitrack.dto.AddFavoriteRequest;
import hu.anitrack.dto.FavoriteResponse;
import hu.anitrack.security.AuthenticatedUser;
import hu.anitrack.service.FavoriteService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public List<FavoriteResponse> getFavorites(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return favoriteService.getFavorites(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteResponse add(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddFavoriteRequest request) {
        return favoriteService.add(user.id(), request);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long mediaId) {
        favoriteService.remove(user.id(), mediaId);
    }
}