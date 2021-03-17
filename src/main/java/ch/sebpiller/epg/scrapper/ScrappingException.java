package ch.sebpiller.epg.scrapper;

public class ScrappingException extends RuntimeException {
    public ScrappingException() {
    }

    public ScrappingException(String message) {
        super(message);
    }

    public ScrappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScrappingException(Throwable cause) {
        super(cause);
    }

    public ScrappingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
