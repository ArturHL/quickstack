package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Returns HTTP 404 Not Found.
 * <p>
 * The error code is generated from the resource type (e.g., "Category" → "CATEGORY_NOT_FOUND").
 * This allows clients to distinguish between different types of missing resources.
 * <p>
 * Example usage:
 * <pre>
 * throw new ResourceNotFoundException("Category", categoryId);
 * // Results in: HTTP 404, code="CATEGORY_NOT_FOUND", message="Category not found: {uuid}"
 * </pre>
 * <p>
 * ASVS V4: Access control - returns 404 instead of 403 for cross-tenant resources
 * to avoid revealing their existence.
 */
public class ResourceNotFoundException extends ApiException {

    /**
     * Creates a new resource not found exception with a UUID identifier.
     *
     * @param resourceType the type of resource (e.g., "Category", "Product")
     * @param id the UUID of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, UUID id) {
        super(
            HttpStatus.NOT_FOUND,
            resourceType.toUpperCase() + "_NOT_FOUND",
            String.format("%s not found: %s", resourceType, id)
        );
    }

    /**
     * Creates a new resource not found exception with a string identifier.
     *
     * @param resourceType the type of resource (e.g., "User", "Tenant")
     * @param identifier the identifier of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
            HttpStatus.NOT_FOUND,
            resourceType.toUpperCase() + "_NOT_FOUND",
            String.format("%s not found: %s", resourceType, identifier)
        );
    }

    /**
     * Creates a new resource not found exception with a custom message.
     * Uses the generic "RESOURCE_NOT_FOUND" error code.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
