package vn.edu.doculib.exception;

import java.io.Serial;

public class ConcurrentUpdateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException(String message) {
        super(message);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
