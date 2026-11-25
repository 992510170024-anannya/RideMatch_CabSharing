package com.ridematch.exception;

import org.springframework.http.HttpStatus;

/** Signals that a requested database resource does not exist. */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
