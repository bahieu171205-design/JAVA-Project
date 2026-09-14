package vn.edu.doculib.exception;

import java.io.Serial;

public class InvalidAcquisitionTransitionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidAcquisitionTransitionException(String message) {
        super(message);
    }
}
