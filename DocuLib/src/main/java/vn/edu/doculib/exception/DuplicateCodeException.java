package vn.edu.doculib.exception;

import java.io.Serial;

public class DuplicateCodeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateCodeException(String message) {
        super(message);
    }
}
