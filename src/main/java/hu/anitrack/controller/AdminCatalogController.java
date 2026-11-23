package hu.anitrack.controller;

import hu.anitrack.dto.GenreResponse;
import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.MediaRequest;
import hu.anitrack.dto.NameRequest;
import hu.anitrack.dto.TagResponse;
import hu.anitrack.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalog management. SecurityConfig lets only the ADMIN role reach /api/admin/**
 * (no token: 401, a normal USER: 403).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService adminService;

    // ------------------------------------------------------------------ media

    @PostMapping("/media")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaDetailResponse createMedia(@Valid @RequestBody MediaRequest request) {
        return adminService.createMedia(request);
    }

    @PutMapping("/media/{id}")
    public MediaDetailResponse updateMedia(@PathVariable Long id, @Valid @RequestBody MediaRequest request) {
        return adminService.updateMedia(id, request);
    }

    @DeleteMapping("/media/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable Long id) {
        adminService.deleteMedia(id);
    }

    // ----------------------------------------------------------------- genres

    @PostMapping("/genres")
    @ResponseStatus(HttpStatus.CREATED)
    public GenreResponse createGenre(@Valid @RequestBody NameRequest request) {
        return adminService.createGenre(request);
    }

    @PutMapping("/genres/{id}")
    public GenreResponse updateGenre(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminService.updateGenre(id, request);
    }

    @DeleteMapping("/genres/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGenre(@PathVariable Long id) {
        adminService.deleteGenre(id);
    }

    // ------------------------------------------------------------------- tags

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody NameRequest request) {
        return adminService.createTag(request);
    }

    @PutMapping("/tags/{id}")
    public TagResponse updateTag(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminService.updateTag(id, request);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        adminService.deleteTag(id);
    }
}