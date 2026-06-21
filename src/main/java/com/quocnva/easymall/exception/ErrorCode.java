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

    // ── Category ─────────────────────────────────────────────────────────
    CATEGORY_NOT_FOUND(6001, "error.category-not-found", HttpStatus.NOT_FOUND),
    CATEGORY_CODE_ALREADY_EXISTS(6002, "error.category-code-already-exists", HttpStatus.CONFLICT),
    PARENT_CATEGORY_NOT_FOUND(6003, "error.parent-category-not-found", HttpStatus.BAD_REQUEST),
    MAX_LEVEL_EXCEEDED(6004, "error.max-level-exceeded", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_CHILDREN(6005, "error.category-has-children", HttpStatus.CONFLICT),
    CATEGORY_IN_USE(6006, "error.category-in-use", HttpStatus.CONFLICT),

    // ── Product ──────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND(7001, "error.product-not-found", HttpStatus.NOT_FOUND),
    PRODUCT_SLUG_ALREADY_EXISTS(7002, "error.product-slug-already-exists", HttpStatus.CONFLICT),
    PRODUCT_VARIANT_NOT_FOUND(7003, "error.product-variant-not-found", HttpStatus.NOT_FOUND),
    SKU_ALREADY_EXISTS(7004, "error.sku-already-exists", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND_FOR_PRODUCT(7005, "error.category-not-found-for-product", HttpStatus.BAD_REQUEST),
    INVALID_STOCK_QUANTITY(7006, "error.invalid-stock-quantity", HttpStatus.BAD_REQUEST),
    INVALID_JSONB_FORMAT(7007, "error.invalid-jsonb-format", HttpStatus.BAD_REQUEST),
    ;


    private final int code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public String getMessage() {
        return Translator.toLocale(messageKey);
    }
}
