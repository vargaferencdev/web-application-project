package hu.anitrack.exception;

/** The user tried to touch data that belongs to someone else (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}