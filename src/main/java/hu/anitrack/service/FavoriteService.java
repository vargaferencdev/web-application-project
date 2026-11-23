package hu.anitrack.service;

import hu.anitrack.dto.AddFavoriteRequest;
import hu.anitrack.dto.FavoriteResponse;
import hu.anitrack.entity.Favorite;
import hu.anitrack.entity.Media;
import hu.anitrack.exception.BusinessRuleException;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.mapper.FavoriteMapper;
import hu.anitrack.repository.FavoriteRepository;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    // the database enforces the same limit (CHECK position BETWEEN 1 AND 6);
    // checking here as well gives the user a clear error message
    private static final int MAX_FAVORITES = 6;

    private final FavoriteRepository favoriteRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final FavoriteMapper favoriteMapper;

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByPositionAsc(userId).stream()
                .map(favoriteMapper::toResponse)
                .toList();
    }

    @Transactional
    public FavoriteResponse add(Long userId, AddFavoriteRequest request) {
        Media media = mediaRepository.findById(request.mediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + request.mediaId()));

        if (favoriteRepository.findByUserIdAndMediaId(userId, media.getId()).isPresent()) {
            throw new DuplicateResourceException("This title is already among your favorites");
        }

        List<Favorite> current = favoriteRepository.findByUserIdOrderByPositionAsc(userId);
        if (current.size() >= MAX_FAVORITES) {
            throw new BusinessRuleException("You can have at most " + MAX_FAVORITES + " favorites");
        }

        // lowest free position (positions can have gaps after a removal)
        Set<Integer> used = current.stream()
                .map(f -> f.getPosition().intValue())
                .collect(Collectors.toSet());
        int position = 1;
        while (used.contains(position)) {
            position++;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(userRepository.getReferenceById(userId));
        favorite.setMedia(media);
        favorite.setPosition((short) position);

        return favoriteMapper.toResponse(favoriteRepository.saveAndFlush(favorite));
    }

    @Transactional
    public void remove(Long userId, Long mediaId) {
        Favorite favorite = favoriteRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("This title is not among your favorites"));
        favoriteRepository.delete(favorite);
    }
}