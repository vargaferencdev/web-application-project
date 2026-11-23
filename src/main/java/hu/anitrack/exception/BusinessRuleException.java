package hu.anitrack.exception;

/** A business rule was violated, e.g. more than 6 favorites or progress above the total (HTTP 400). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}