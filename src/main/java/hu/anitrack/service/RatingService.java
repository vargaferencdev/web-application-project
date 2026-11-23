package hu.anitrack.service;

import hu.anitrack.dto.RatingRequest;
import hu.anitrack.dto.RatingResponse;
import hu.anitrack.dto.RatingSummaryResponse;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.Rating;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.repository.MediaRepository;
import hu.anitrack.repository.RatingRepository;
import hu.anitrack.repository.UserRepository;
import hu.anitrack.repository.projection.RatingBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    /** Creates the rating or changes the existing one (one rating per user and title). */
    @Transactional
    public RatingResponse rate(Long userId, Long mediaId, RatingRequest request) {
        Media media = findMedia(mediaId);

        Rating rating = ratingRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseGet(() -> {
                    Rating created = new Rating();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setMedia(media);
                    return created;
                });
        rating.setReaction(request.reaction());

        Rating saved = ratingRepository.saveAndFlush(rating);
        return new RatingResponse(mediaId, saved.getReaction());
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRating(Long userId, Long mediaId) {
        Rating rating = ratingRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not rated this title"));
        return new RatingResponse(mediaId, rating.getReaction());
    }

    @Transactional
    public void remove(Long userId, Long mediaId) {
        Rating rating = ratingRepository.findByUserIdAndMediaId(userId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("You have not rated this title"));
        ratingRepository.delete(rating);
    }

    /** Percentages are computed on demand from the reaction counts, never stored. */
    @Transactional(readOnly = true)
    public RatingSummaryResponse getSummary(Long mediaId) {
        findMedia(mediaId);

        RatingBreakdown breakdown = ratingRepository.getBreakdown(mediaId);
        long total = breakdown.getTotal();
        return new RatingSummaryResponse(
                total,
                breakdown.getPositive(),
                breakdown.getNeutral(),
                breakdown.getNegative(),
                percent(breakdown.getPositive(), total),
                percent(breakdown.getNeutral(), total),
                percent(breakdown.getNegative(), total));
    }

    private double percent(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(1000.0 * part / total) / 10.0;   // one decimal
    }

    private Media findMedia(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));
    }
}