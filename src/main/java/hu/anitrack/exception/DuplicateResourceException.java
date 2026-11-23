package hu.anitrack.exception;

/** The record already exists, e.g. taken username or title already on the list (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}