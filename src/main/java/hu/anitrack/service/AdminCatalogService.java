package hu.anitrack.service;

import hu.anitrack.dto.GenreResponse;
import hu.anitrack.dto.MediaDetailResponse;
import hu.anitrack.dto.MediaRequest;
import hu.anitrack.dto.NameRequest;
import hu.anitrack.dto.TagResponse;
import hu.anitrack.entity.Genre;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.Tag;
import hu.anitrack.entity.enums.MediaFormat;
import hu.anitrack.entity.enums.MediaStatus;
import hu.anitrack.entity.enums.MediaType;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.repository.GenreRepository;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.TagRepository;
import hu.anitrack.repository.projection.GenreCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Catalog management for admins: titles, genres and tags. */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    // same rules as the CHECK constraints in V1__init.sql
    private static final Map<MediaType, Set<MediaFormat>> FORMATS = Map.of(
            MediaType.ANIME, EnumSet.of(MediaFormat.TV, MediaFormat.MOVIE, MediaFormat.OVA,
                    MediaFormat.ONA, MediaFormat.SPECIAL),
            MediaType.MANGA, EnumSet.of(MediaFormat.MANGA, MediaFormat.ONE_SHOT, MediaFormat.NOVEL));

    private static final Map<MediaType, Set<MediaStatus>> STATUSES = Map.of(
            MediaType.ANIME, EnumSet.of(MediaStatus.UPCOMING, MediaStatus.AIRING,
                    MediaStatus.FINISHED, MediaStatus.HIATUS),
            MediaType.MANGA, EnumSet.of(MediaStatus.UPCOMING, MediaStatus.RELEASING,
                    MediaStatus.FINISHED, MediaStatus.HIATUS));

    private final MediaRepository mediaRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final RatingService ratingService;

    // ------------------------------------------------------------------ media

    @Transactional
    public MediaDetailResponse createMedia(MediaRequest request) {
        validate(request);
        Media media = new Media();
        apply(media, request);
        return toDetail(mediaRepository.saveAndFlush(media));
    }

    /** PUT semantics: every field is replaced by the request. */
    @Transactional
    public MediaDetailResponse updateMedia(Long id, MediaRequest request) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + id));
        validate(request);
        apply(media, request);
        return toDetail(mediaRepository.saveAndFlush(media));
    }

    /**
     * Deleting a title also removes it from every list, rating, favorite, genre and tag link:
     * the database does this with ON DELETE CASCADE.
     */
    @Transactional
    public void deleteMedia(Long id) {
        if (!mediaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Media not found: " + id);
        }
        mediaRepository.deleteById(id);
    }

    // ----------------------------------------------------------------- genres

    @Transactional
    public GenreResponse createGenre(NameRequest request) {
        String name = request.name().trim();
        if (genreRepository.existsByName(name)) {
            throw new DuplicateResourceException("Genre already exists: " + name);
        }
        Genre genre = new Genre();
        genre.setName(name);
        Genre saved = genreRepository.saveAndFlush(genre);
        return new GenreResponse(saved.getId(), saved.getName(), 0L);
    }

    @Transactional
    public GenreResponse updateGenre(Long id, NameRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found: " + id));
        String name = request.name().trim();
        genreRepository.findByName(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Genre already exists: " + name);
                });
        genre.setName(name);
        Genre saved = genreRepository.saveAndFlush(genre);
        return new GenreResponse(saved.getId(), saved.getName(), genreCount(id));
    }

    @Transactional
    public void deleteGenre(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Genre not found: " + id);
        }
        genreRepository.deleteById(id);   // the links in media_genres are removed by the database
    }

    // ------------------------------------------------------------------- tags

    @Transactional
    public TagResponse createTag(NameRequest request) {
        String name = request.name().trim();
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag already exists: " + name);
        }
        Tag tag = new Tag();
        tag.setName(name);
        Tag saved = tagRepository.saveAndFlush(tag);
        return new TagResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public TagResponse updateTag(Long id, NameRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        String name = request.name().trim();
        tagRepository.findByName(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Tag already exists: " + name);
                });
        tag.setName(name);
        Tag saved = tagRepository.saveAndFlush(tag);
        return new TagResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found: " + id);
        }
        tagRepository.deleteById(id);
    }

    // ---------------------------------------------------------------- helpers

    private void validate(MediaRequest r) {
        if (!FORMATS.get(r.mediaType()).contains(r.format())) {
            throw new BusinessRuleException(r.format() + " is not a valid format for " + r.mediaType());
        }
        if (!STATUSES.get(r.mediaType()).contains(r.status())) {
            throw new BusinessRuleException(r.status() + " is not a valid status for " + r.mediaType());
        }
        if (r.mediaType() == MediaType.ANIME) {
            if (r.totalChapters() != null || r.totalVolumes() != null) {
                throw new BusinessRuleException("An anime cannot have chapters or volumes");
            }
        } else {
            if (r.totalEpisodes() != null || r.durationMinutes() != null) {
                throw new BusinessRuleException("A manga cannot have episodes or an episode duration");
            }
        }
        if (r.startDate() != null && r.endDate() != null && r.endDate().isBefore(r.startDate())) {
            throw new BusinessRuleException("The end date cannot be before the start date");
        }
    }

    private void apply(Media media, MediaRequest r) {
        media.setTitle(r.title().trim());
        media.setDescription(blankToNull(r.description()));
        media.setMediaType(r.mediaType());
        media.setFormat(r.format());
        media.setStatus(r.status());
        media.setStartDate(r.startDate());
        media.setEndDate(r.endDate());
        media.setTotalEpisodes(r.totalEpisodes());
        media.setTotalChapters(r.totalChapters());
        media.setTotalVolumes(r.totalVolumes());
        media.setDurationMinutes(r.durationMinutes());
        media.setCoverImageUrl(blankToNull(r.coverImageUrl()));
        media.setGenres(new HashSet<>(findGenres(r.genreIds())));
        media.setTags(new HashSet<>(findTags(r.tagIds())));
    }

    private List<Genre> findGenres(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Genre> found = genreRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more genres do not exist");
        }
        return found;
    }

    private List<Tag> findTags(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Tag> found = tagRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more tags do not exist");
        }
        return found;
    }

    private long genreCount(Long genreId) {
        return genreRepository.countMediaPerGenre().stream()
                .filter(g -> g.getGenreId().equals(genreId))
                .map(GenreCount::getMediaCount)
                .findFirst()
                .orElse(0L);
    }

    private MediaDetailResponse toDetail(Media media) {
        return MediaDetailResponse.from(media, ratingService.getSummary(media.getId()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}