package hu.anitrack.controller;

import hu.anitrack.dto.GenreResponse;
import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.MediaSummaryResponse;
import hu.anitrack.dto.PageResponse;
import hu.anitrack.dto.PopularMediaResponse;
import hu.anitrack.dto.TagResponse;
import hu.anitrack.dto.TopRatedResponse;
import hu.anitrack.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only catalog: anyone can browse, no login needed. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/media")
    public PageResponse<MediaSummaryResponse> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalogService.search(type, title, genreId, year, page, size);
    }

    @GetMapping("/media/{id}")
    public MediaDetailResponse getById(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @GetMapping("/media/popular")
    public List<PopularMediaResponse> popular(@RequestParam(defaultValue = "10") int limit) {
        return catalogService.popular(limit);
    }

    @GetMapping("/media/top-rated")
    public List<TopRatedResponse> topRated(@RequestParam(defaultValue = "2") int minVotes,
                                           @RequestParam(defaultValue = "10") int limit) {
        return catalogService.topRated(minVotes, limit);
    }

    @GetMapping("/genres")
    public List<GenreResponse> genres() {
        return catalogService.genres();
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return catalogService.tags();
    }
}