package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            String.format("%s not found: %s", resourceType, identifier)
        );
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
