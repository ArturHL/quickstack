package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a resource with a duplicate unique field.
 * <p>
 * This exception is used for business rule violations related to uniqueness constraints,
 * such as duplicate names, SKUs, or email addresses.
 * <p>
 * Returns HTTP 409 Conflict.
 * <p>
 * Example usage:
 * <pre>
 * throw new DuplicateResourceException("Category", "name", "Bebidas");
 * // Results in: HTTP 409, code="DUPLICATE_NAME", message="Category with name 'Bebidas' already exists"
 * </pre>
 */
public class DuplicateResourceException extends ApiException {

    /**
     * Creates a new duplicate resource exception.
     *
     * @param resourceType the type of resource (e.g., "Category", "Product")
     * @param field the field that has the duplicate value (e.g., "name", "sku")
     * @param value the duplicate value that was attempted
     */
    public DuplicateResourceException(String resourceType, String field, String value) {
        super(
            HttpStatus.CONFLICT,
            "DUPLICATE_" + field.toUpperCase(),
            String.format("%s with %s '%s' already exists", resourceType, field, value)
        );
    }
}
