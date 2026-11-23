package hu.anitrack.controller;

import hu.anitrack.dto.AddToListRequest;
import hu.anitrack.dto.ListEntryResponse;
import hu.anitrack.dto.UpdateListEntryRequest;
import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.security.AuthenticatedUser;
import hu.anitrack.service.UserMediaListService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The logged-in user's own list. The user id always comes from the token, never from the request. */
@RestController
@RequestMapping("/api/me/list")
@RequiredArgsConstructor
public class ListController {

    private final UserMediaListService listService;

    @GetMapping
    public List<ListEntryResponse> getList(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) ListStatus status) {
        return listService.getList(user.id(), status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListEntryResponse add(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddToListRequest request) {
        return listService.add(user.id(), request);
    }

    @PatchMapping("/{entryId}")
    public ListEntryResponse update(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long entryId,
            @Valid @RequestBody UpdateListEntryRequest request) {
        return listService.update(user.id(), entryId, request);
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long entryId) {
        listService.delete(user.id(), entryId);
    }
}