package com.quocnva.easymall.exception;

import com.quocnva.easymall.util.Translator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────────────────
    UNCATEGORIZED_EXCEPTION(9999, "error.uncategorized", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Auth ─────────────────────────────────────────────────────────────
    INVALID_CREDENTIALS(1001, "error.invalid-credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1002, "error.token-invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(1003, "error.token-not-found", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_ACTIVE(1004, "error.account-not-active", HttpStatus.FORBIDDEN),
    OTP_INVALID(1005, "error.otp-invalid", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_SENT(1006, "error.otp-already-sent", HttpStatus.TOO_MANY_REQUESTS),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(2001, "error.user-not-found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2002, "error.email-already-exists", HttpStatus.CONFLICT),

    // ── Resource ─────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND(3001, "error.resource-not-found", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(3002, "error.access-denied", HttpStatus.FORBIDDEN),

    // ── Rate Limiting & Token ─────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(1007, "error.rate-limit-exceeded", HttpStatus.TOO_MANY_REQUESTS),
    REFRESH_TOKEN_INVALID(1008, "error.refresh-token-invalid", HttpStatus.UNAUTHORIZED),
    PENDING_REGISTRATION_NOT_FOUND(1009, "error.pending-registration-not-found", HttpStatus.NOT_FOUND),
    ACCOUNT_ALREADY_ACTIVE(1010, "error.account-already-active", HttpStatus.CONFLICT),

    // ── Role ─────────────────────────────────────────────────────────────
    ROLE_NOT_FOUND(4001, "error.role-not-found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(4002, "error.role-already-exists", HttpStatus.CONFLICT),
    ROLE_PROTECTED(4003, "error.role-protected", HttpStatus.FORBIDDEN),

    // ── Permission ───────────────────────────────────────────────────────
    PERMISSION_NOT_FOUND(5001, "error.permission-not-found", HttpStatus.NOT_FOUND),
    PERMISSION_ALREADY_EXISTS(5002, "error.permission-already-exists", HttpStatus.CONFLICT),
    PERMISSION_IN_USE(5003, "error.permission-in-use", HttpStatus.CONFLICT),
    ;


    private final int code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public String getMessage() {
        return Translator.toLocale(messageKey);
    }
}
