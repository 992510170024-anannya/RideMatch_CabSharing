package com.ridematch.exception;

import org.springframework.http.HttpStatus;

/** Signals an attempted operation the authenticated application role cannot perform. */
public class ForbiddenOperationException extends ApplicationException {

    public ForbiddenOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
