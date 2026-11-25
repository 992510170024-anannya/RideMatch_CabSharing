package com.ridematch.exception;

import org.springframework.http.HttpStatus;

/** Signals a duplicate or conflicting state change. */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
