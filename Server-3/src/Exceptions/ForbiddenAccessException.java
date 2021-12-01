package Exceptions;

public class ForbiddenAccessException extends Exception {
    public ForbiddenAccessException() {
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }
}