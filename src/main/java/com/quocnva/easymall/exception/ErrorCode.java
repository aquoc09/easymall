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
    INVALID_REQUEST(1000, "error.invalid-request", HttpStatus.BAD_REQUEST),

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
    INVALID_STATUS_TRANSITION(3003, "error.invalid-status-transition", HttpStatus.BAD_REQUEST),

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

    // ── Cart ─────────────────────────────────────────────────────────────
    CART_NOT_FOUND(8001, "error.cart-not-found", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(8002, "error.cart-item-not-found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(8003, "error.insufficient-stock", HttpStatus.BAD_REQUEST),
    MAX_ORDER_QUANTITY_EXCEEDED(8004, "error.max-order-quantity-exceeded", HttpStatus.BAD_REQUEST),
    PRODUCT_BANNED(8005, "error.product-banned", HttpStatus.BAD_REQUEST),

    // ── Coupon ───────────────────────────────────────────────────────────────
    COUPON_NOT_FOUND(9001, "error.coupon-not-found", HttpStatus.NOT_FOUND),
    COUPON_CODE_ALREADY_EXISTS(9002, "error.coupon-code-already-exists", HttpStatus.CONFLICT),
    COUPON_EXPIRED(9003, "error.coupon-expired", HttpStatus.BAD_REQUEST),
    COUPON_EXHAUSTED(9004, "error.coupon-exhausted", HttpStatus.BAD_REQUEST),
    COUPON_USAGE_LIMIT_EXCEEDED(9005, "error.coupon-usage-limit-exceeded", HttpStatus.BAD_REQUEST),
    INADEQUATE_ORDER_VALUE(9006, "error.inadequate-order-value", HttpStatus.BAD_REQUEST),
    BUDGET_EXCEEDED(9007, "error.budget-exceeded", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD_FOR_COUPON(9008, "error.invalid-payment-method-for-coupon", HttpStatus.BAD_REQUEST),

    // ── Order ─────────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(10001, "error.order-not-found", HttpStatus.NOT_FOUND),
    ORDER_STATE_CONFLICT(10002, "error.order-state-conflict", HttpStatus.CONFLICT),
    CANCELLATION_NOT_ALLOWED(10003, "error.cancellation-not-allowed", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATE(10004, "error.invalid-order-state", HttpStatus.BAD_REQUEST),
    CHECKOUT_CONCURRENCY(10005, "error.checkout-concurrency", HttpStatus.CONFLICT),
    ORDER_OWNERSHIP_DENIED(10006, "error.order-ownership-denied", HttpStatus.FORBIDDEN),
    CART_ITEMS_NOT_FOUND(10007, "error.cart-items-not-found", HttpStatus.BAD_REQUEST),
    ADDRESS_NOT_FOUND(10008, "error.address-not-found", HttpStatus.NOT_FOUND),
    FRAUD_DETECTED(10009, "error.fraud-detected", HttpStatus.FORBIDDEN),
    PAYMENT_URL_CREATION_FAILED(10010, "error.payment-url-creation-failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── Review ────────────────────────────────────────────────────────────────
    REVIEW_NOT_FOUND(11001, "error.review-not-found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(11002, "error.review-already-exists", HttpStatus.CONFLICT),
    REVIEW_ORDER_NOT_COMPLETED(11003, "error.review-order-not-completed", HttpStatus.BAD_REQUEST),
    REVIEW_ORDER_OWNERSHIP_DENIED(11004, "error.review-order-ownership-denied", HttpStatus.FORBIDDEN),

    // ── Wishlist ─────────────────────────────────────────────────────────────────────────────
    WISHLIST_ITEM_NOT_FOUND(12001, "error.wishlist-item-not-found", HttpStatus.NOT_FOUND),

    // ── Address ──────────────────────────────────────────────────────────────────────────
    ADDRESS_OWNERSHIP_DENIED(14001, "error.address-ownership-denied", HttpStatus.FORBIDDEN),
    ADDRESS_MAX_LIMIT(14002, "error.address-max-limit", HttpStatus.BAD_REQUEST),

    // ── GHN ───────────────────────────────────────────────────────────────────────────────────
    GHN_INTEGRATION_ERROR(13001, "error.ghn-integration", HttpStatus.INTERNAL_SERVER_ERROR),
    GHN_INVALID_LOCATION(13002, "error.ghn-invalid-location", HttpStatus.BAD_REQUEST),
    GHN_SERVICE_UNAVAILABLE(13003, "error.ghn-service-unavailable", HttpStatus.BAD_REQUEST),

    // ── Upload ──────────────────────────────────────────────────────────────────────────────
    INVALID_FILE_TYPE(15001, "error.upload.invalid-file-type", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(15002, "error.upload.file-too-large", HttpStatus.PAYLOAD_TOO_LARGE),
    S3_UPLOAD_FAILURE(15003, "error.upload.s3-failure", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_FILE(15004, "error.upload.empty-file", HttpStatus.BAD_REQUEST),
    // ── Slider ──────────────────────────────────────────────────────────────────────────────
    SLIDER_NOT_FOUND(16001, "error.slider-not-found", HttpStatus.NOT_FOUND),
    ;


    private final int code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    public String getMessage() {
        return Translator.toLocale(messageKey);
    }
}
