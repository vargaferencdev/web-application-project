package hu.anitrack.repository.projection;

public interface RatingSummary {

    Long getMediaId();

    String getTitle();

    String getMediaType();

    Long getVotes();

    Double getPositivePercent();
}
