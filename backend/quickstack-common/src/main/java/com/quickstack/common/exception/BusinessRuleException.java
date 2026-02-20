package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a business rule is violated.
 * <p>
 * Used for domain-level constraints that go beyond simple validation,
 * such as attempting to delete a category that has active products.
 * <p>
 * Returns HTTP 409 Conflict.
 * <p>
 * Example usage:
 * <pre>
 * throw new BusinessRuleException("CATEGORY_HAS_PRODUCTS",
 *     "Cannot delete category: it has 3 active products");
 * </pre>
 */
public class BusinessRuleException extends ApiException {

    /**
     * Creates a new business rule exception.
     *
     * @param code a machine-readable error code (e.g., "CATEGORY_HAS_PRODUCTS")
     * @param message a human-readable description of the violated rule
     */
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
