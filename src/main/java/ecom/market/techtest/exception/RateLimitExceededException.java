package ecom.market.techtest.exception;

public class RateLimitExceededException extends Exception {

    public RateLimitExceededException() {
        super();
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
