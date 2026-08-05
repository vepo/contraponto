package dev.vepo.contraponto.reddit;

public class RedditApiException extends RuntimeException {

    public RedditApiException(String message) {
        super(message);
    }

    public RedditApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
