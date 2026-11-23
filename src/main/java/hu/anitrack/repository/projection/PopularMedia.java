package hu.anitrack.repository.projection;

public interface PopularMedia {

    Long getMediaId();

    String getTitle();

    String getMediaType();

    Long getListCount();
}
