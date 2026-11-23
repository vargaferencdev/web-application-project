package hu.anitrack.service;

import hu.anitrack.dto.GenreResponse;
import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.MediaSummaryResponse;
import hu.anitrack.dto.PageResponse;
import hu.anitrack.dto.PopularMediaResponse;
import hu.anitrack.dto.RatingSummaryResponse;
import hu.anitrack.dto.TagResponse;
import hu.anitrack.dto.TopRatedResponse;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.enums.MediaType;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.mapper.MediaMapper;
import hu.anitrack.repository.GenreRepository;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.RatingRepository;
import hu.anitrack.repository.TagRepository;
import hu.anitrack.repository.UserMediaListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The public, read-only side of the catalog. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_LIST_SIZE = 100;

    private final MediaRepository mediaRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final RatingRepository ratingRepository;
    private final UserMediaListRepository listRepository;
    private final RatingService ratingService;
    private final MediaMapper mediaMapper;

    @Transactional(readOnly = true)
    public PageResponse<MediaSummaryResponse> search(String type, String title, Long genreId,
                                                     Integer year, int page, int size) {
        String normalizedType = normalizeType(type);
        String normalizedTitle = (title == null || title.isBlank()) ? null : title.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        Page<Media> result = mediaRepository.search(
                normalizedType, normalizedTitle, genreId, year, PageRequest.of(safePage, safeSize));
        return PageResponse.from(result.map(mediaMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public MediaDetailResponse getById(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + id));
        RatingSummaryResponse ratings = ratingService.getSummary(id);
        return MediaDetailResponse.from(media, ratings);
    }

    @Transactional(readOnly = true)
    public List<PopularMediaResponse> popular(int limit) {
        return listRepository.findMostPopular(clampLimit(limit)).stream()
                .map(p -> new PopularMediaResponse(p.getMediaId(), p.getTitle(), p.getMediaType(), p.getListCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopRatedResponse> topRated(int minVotes, int limit) {
        return ratingRepository.findTopRated(Math.max(minVotes, 1), clampLimit(limit)).stream()
                .map(r -> new TopRatedResponse(r.getMediaId(), r.getTitle(), r.getMediaType(),
                        r.getVotes(), r.getPositivePercent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> genres() {
        return genreRepository.countMediaPerGenre().stream()
                .map(g -> new GenreResponse(g.getGenreId(), g.getName(), g.getMediaCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tags() {
        return tagRepository.findAll(Sort.by("name")).stream()
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .toList();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return MediaType.valueOf(type.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown media type: " + type + " (use ANIME or MANGA)");
        }
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIST_SIZE));
    }
}