package hu.anitrack.repository.projection;

public interface RatingBreakdown {

    Long getTotal();

    Long getPositive();

    Long getNeutral();

    Long getNegative();
}