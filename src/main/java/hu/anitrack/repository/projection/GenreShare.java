package hu.anitrack.repository.projection;

/** One genre of a user's list and how many of their titles have it. */
public interface GenreShare {
    String getName();
    Long getTitleCount();
}
