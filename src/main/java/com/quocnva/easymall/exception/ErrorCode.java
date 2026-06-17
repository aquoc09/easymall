package com.quocnva.easymall.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────────────────
    UNCATEGORIZED_EXCEPTION(9999, "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Auth ─────────────────────────────────────────────────────────────
    INVALID_CREDENTIALS(1001, "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1002, "Token is invalid or expired", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(1003, "Token not found", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_ACTIVE(1004, "Account is not activated yet", HttpStatus.FORBIDDEN),
    OTP_INVALID(1005, "OTP is invalid or expired", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_SENT(1006, "OTP already sent, please wait before requesting again", HttpStatus.TOO_MANY_REQUESTS),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(2001, "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2002, "Email is already registered", HttpStatus.CONFLICT),

    // ── Resource ─────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND(3001, "Resource not found", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(3002, "You do not have permission to perform this action", HttpStatus.FORBIDDEN),

    // ── Rate Limiting & Token ─────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(1007, "Too many requests, please try again later", HttpStatus.TOO_MANY_REQUESTS),
    REFRESH_TOKEN_INVALID(1008, "Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED),
    PENDING_REGISTRATION_NOT_FOUND(1009, "No pending registration found for this email", HttpStatus.NOT_FOUND),
    ACCOUNT_ALREADY_ACTIVE(1010, "Account is already active", HttpStatus.CONFLICT),
    ;


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
