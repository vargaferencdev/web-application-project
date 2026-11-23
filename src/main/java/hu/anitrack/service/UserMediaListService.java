package hu.anitrack.service;

import hu.anitrack.dto.AddToListRequest;
import hu.anitrack.dto.ListEntryResponse;
import hu.anitrack.dto.UpdateListEntryRequest;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.UserMediaList;
import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.entity.enums.MediaType;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ForbiddenOperationException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.mapper.ListEntryMapper;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.UserMediaListRepository;
import hu.anitrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserMediaListService {

    private final UserMediaListRepository listRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final ListEntryMapper listEntryMapper;

    @Transactional(readOnly = true)
    public List<ListEntryResponse> getList(Long userId, ListStatus status) {
        List<UserMediaList> entries = (status == null)
                ? listRepository.findByUserId(userId)
                : listRepository.findByUserIdAndStatus(userId, status);
        return entries.stream().map(listEntryMapper::toResponse).toList();
    }

    @Transactional
    public ListEntryResponse add(Long userId, AddToListRequest request) {
        Media media = mediaRepository.findById(request.mediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + request.mediaId()));

        if (listRepository.existsByUserIdAndMediaId(userId, media.getId())) {
            throw new DuplicateResourceException("This title is already on your list");
        }

        int progress = request.progress() == null ? 0 : request.progress();
        checkProgress(media, progress);

        UserMediaList entry = new UserMediaList();
        entry.setUser(userRepository.getReferenceById(userId));
        entry.setMedia(media);
        entry.setStatus(request.status());
        entry.setProgress(progress);
        applyStatusRules(entry);

        return listEntryMapper.toResponse(listRepository.saveAndFlush(entry));
    }

    @Transactional
    public ListEntryResponse update(Long userId, Long entryId, UpdateListEntryRequest request) {
        UserMediaList entry = findOwned(userId, entryId);

        // validate first, change afterwards
        if (request.progress() != null) {
            checkProgress(entry.getMedia(), request.progress());
            entry.setProgress(request.progress());
        }
        if (request.status() != null) {
            entry.setStatus(request.status());
        }
        applyStatusRules(entry);

        return listEntryMapper.toResponse(listRepository.saveAndFlush(entry));
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        listRepository.delete(findOwned(userId, entryId));
    }

    // ---- ownership check: a user can only touch their own list entries ----
    private UserMediaList findOwned(Long userId, Long entryId) {
        UserMediaList entry = listRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("List entry not found: " + entryId));
        if (!entry.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("This list entry belongs to another user");
        }
        return entry;
    }

    // ---- business rules ----
    private void checkProgress(Media media, int progress) {
        Integer total = totalOf(media);
        if (total != null && progress > total) {
            throw new BusinessRuleException(
                    "Progress (" + progress + ") cannot exceed the total (" + total + ")");
        }
    }

    /** Keeps status, progress and dates consistent with each other. */
    private void applyStatusRules(UserMediaList entry) {
        Integer total = totalOf(entry.getMedia());
        LocalDate today = LocalDate.now();

        // reaching the last episode/chapter means the title is completed
        if (entry.getStatus() == ListStatus.CURRENT && total != null && entry.getProgress().equals(total)) {
            entry.setStatus(ListStatus.COMPLETED);
        }
        // a completed title has all episodes/chapters done
        if (entry.getStatus() == ListStatus.COMPLETED && total != null) {
            entry.setProgress(total);
        }
        if (entry.getStatus() != ListStatus.PLANNING && entry.getStartedAt() == null) {
            entry.setStartedAt(today);
        }
        if (entry.getStatus() == ListStatus.COMPLETED) {
            if (entry.getCompletedAt() == null) {
                entry.setCompletedAt(today);
            }
        } else {
            entry.setCompletedAt(null);
        }
    }

    private Integer totalOf(Media media) {
        return media.getMediaType() == MediaType.ANIME
                ? media.getTotalEpisodes()
                : media.getTotalChapters();
    }
}