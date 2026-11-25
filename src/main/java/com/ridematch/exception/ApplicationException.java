package com.ridematch.exception;

import org.springframework.http.HttpStatus;

/** Base for expected application errors that need an HTTP status. */
public abstract class ApplicationException extends RuntimeException {

    private final HttpStatus status;

    protected ApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
