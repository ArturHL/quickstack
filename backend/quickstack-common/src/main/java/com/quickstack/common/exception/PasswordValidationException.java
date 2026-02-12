package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when password validation fails.
 * This includes policy violations and breach check failures.
 */
public class PasswordValidationException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private final ValidationFailure failure;

    /**
     * Types of password validation failures.
     */
    public enum ValidationFailure {
        TOO_SHORT("Password must be at least %d characters"),
        TOO_LONG("Password cannot exceed %d characters"),
        COMPROMISED("This password has been found in a data breach"),
        BREACH_CHECK_UNAVAILABLE("Password validation service is temporarily unavailable"),
        SAME_AS_CURRENT("New password must be different from current password");

        private final String messageTemplate;

        ValidationFailure(String messageTemplate) {
            this.messageTemplate = messageTemplate;
        }

        public String getMessageTemplate() {
            return messageTemplate;
        }
    }

    /**
     * Creates a password validation exception.
     *
     * @param failure the type of validation failure
     */
    public PasswordValidationException(ValidationFailure failure) {
        super(STATUS, "PASSWORD_" + failure.name(), failure.getMessageTemplate());
        this.failure = failure;
    }

    /**
     * Creates a password validation exception with formatted message.
     *
     * @param failure the type of validation failure
     * @param args arguments for message formatting
     */
    public PasswordValidationException(ValidationFailure failure, Object... args) {
        super(STATUS, "PASSWORD_" + failure.name(), String.format(failure.getMessageTemplate(), args));
        this.failure = failure;
    }

    public ValidationFailure getFailure() {
        return failure;
    }

    /**
     * Creates exception for password too short.
     */
    public static PasswordValidationException tooShort(int minLength) {
        return new PasswordValidationException(ValidationFailure.TOO_SHORT, minLength);
    }

    /**
     * Creates exception for password too long.
     */
    public static PasswordValidationException tooLong(int maxLength) {
        return new PasswordValidationException(ValidationFailure.TOO_LONG, maxLength);
    }

    /**
     * Creates exception for compromised password.
     */
    public static PasswordValidationException compromised() {
        return new PasswordValidationException(ValidationFailure.COMPROMISED);
    }

    /**
     * Creates exception when breach check service is unavailable.
     */
    public static PasswordValidationException breachCheckUnavailable() {
        return new PasswordValidationException(ValidationFailure.BREACH_CHECK_UNAVAILABLE);
    }

    /**
     * Creates exception when new password is same as current.
     */
    public static PasswordValidationException sameAsCurrent() {
        return new PasswordValidationException(ValidationFailure.SAME_AS_CURRENT);
    }
}
