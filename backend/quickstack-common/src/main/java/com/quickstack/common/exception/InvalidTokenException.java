package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a token (JWT, refresh, or reset) is invalid.
 *
 * ASVS V3.5.3: Verify that tokens are properly validated.
 */
public class InvalidTokenException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;
    private static final String DEFAULT_CODE = "INVALID_TOKEN";

    private final TokenType tokenType;
    private final InvalidationReason reason;

    /**
     * Token types that can be invalid.
     */
    public enum TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        PASSWORD_RESET_TOKEN,
        EMAIL_VERIFICATION_TOKEN
    }

    /**
     * Reasons why a token might be invalid.
     */
    public enum InvalidationReason {
        EXPIRED,
        MALFORMED,
        SIGNATURE_INVALID,
        REVOKED,
        ALREADY_USED,
        NOT_FOUND,
        WRONG_ALGORITHM,
        ISSUER_MISMATCH,
        TENANT_MISMATCH
    }

    /**
     * Creates an invalid token exception.
     *
     * @param tokenType type of token that was invalid
     * @param reason reason for invalidation
     */
    public InvalidTokenException(TokenType tokenType, InvalidationReason reason) {
        super(STATUS, DEFAULT_CODE, buildMessage(tokenType, reason));
        this.tokenType = tokenType;
        this.reason = reason;
    }

    /**
     * Creates an invalid token exception with custom message.
     *
     * @param tokenType type of token that was invalid
     * @param reason reason for invalidation
     * @param message custom error message
     */
    public InvalidTokenException(TokenType tokenType, InvalidationReason reason, String message) {
        super(STATUS, DEFAULT_CODE, message);
        this.tokenType = tokenType;
        this.reason = reason;
    }

    /**
     * Creates an invalid token exception with cause.
     *
     * @param tokenType type of token that was invalid
     * @param reason reason for invalidation
     * @param cause the underlying cause
     */
    public InvalidTokenException(TokenType tokenType, InvalidationReason reason, Throwable cause) {
        super(STATUS, DEFAULT_CODE, buildMessage(tokenType, reason), cause);
        this.tokenType = tokenType;
        this.reason = reason;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public InvalidationReason getReason() {
        return reason;
    }

    private static String buildMessage(TokenType tokenType, InvalidationReason reason) {
        // Generic message to avoid leaking information about token validation
        return switch (reason) {
            case EXPIRED -> "Token has expired";
            case REVOKED -> "Token has been revoked";
            case ALREADY_USED -> "Token has already been used";
            default -> "Invalid token";
        };
    }

    /**
     * Creates exception for expired token.
     */
    public static InvalidTokenException expired(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.EXPIRED);
    }

    /**
     * Creates exception for revoked token.
     */
    public static InvalidTokenException revoked(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.REVOKED);
    }

    /**
     * Creates exception for already used token (single-use tokens).
     */
    public static InvalidTokenException alreadyUsed(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.ALREADY_USED);
    }

    /**
     * Creates exception for malformed token.
     */
    public static InvalidTokenException malformed(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.MALFORMED);
    }

    /**
     * Creates exception for invalid signature.
     */
    public static InvalidTokenException invalidSignature(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.SIGNATURE_INVALID);
    }

    /**
     * Creates exception for wrong algorithm (e.g., algorithm confusion attack).
     */
    public static InvalidTokenException wrongAlgorithm(TokenType tokenType) {
        return new InvalidTokenException(tokenType, InvalidationReason.WRONG_ALGORITHM);
    }
}
