package dev.vepo.passport.shared.infra;

public class PassportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PassportException(String message) {
        super(message);
    }

    public PassportException(String message, Throwable cause) {
        super(message, cause);
    }

    public PassportException(Throwable cause) {
        super(cause);
    }

}