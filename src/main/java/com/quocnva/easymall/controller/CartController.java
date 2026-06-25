package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.cart.CartItemRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.cart.CartResponse;
import com.quocnva.easymall.service.cart.CartService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ══════════════════════════════════════════════════════════════════
    // GET /api/v1/carts/me — Lấy giỏ hàng của user đang đăng nhập
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/me")
    @PreAuthorize("@permissionChecker.has('cart:view')")
    public ApiResponse<CartResponse> getMyCart(Authentication authentication) {
        String email = authentication.getName();
        return ApiResponse.<CartResponse>builder()
                .result(cartService.getCart(email))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // POST /api/v1/carts/me/items — Thêm item vào giỏ
    // ══════════════════════════════════════════════════════════════════

    @PostMapping("/me/items")
    @PreAuthorize("@permissionChecker.has('cart:manage')")
    public ApiResponse<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        String email = authentication.getName();
        return ApiResponse.<CartResponse>builder()
                .message(Translator.toLocale("success.cart.item-added"))
                .result(cartService.addItem(email, request))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // PUT /api/v1/carts/me/items/{variantId} — Cập nhật số lượng item
    // ══════════════════════════════════════════════════════════════════

    @PutMapping("/me/items/{variantId}")
    @PreAuthorize("@permissionChecker.has('cart:manage')")
    public ApiResponse<CartResponse> updateItem(
            Authentication authentication,
            @PathVariable Long variantId,
            @Valid @RequestBody CartItemRequest request
    ) {
        String email = authentication.getName();
        return ApiResponse.<CartResponse>builder()
                .message(Translator.toLocale("success.cart.item-updated"))
                .result(cartService.updateItem(email, variantId, request))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // DELETE /api/v1/carts/me/items/{variantId} — Xóa 1 item khỏi giỏ
    // ══════════════════════════════════════════════════════════════════

    @DeleteMapping("/me/items/{variantId}")
    @PreAuthorize("@permissionChecker.has('cart:manage')")
    public ApiResponse<Void> removeItem(
            Authentication authentication,
            @PathVariable Long variantId
    ) {
        String email = authentication.getName();
        cartService.removeItem(email, variantId);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.cart.item-removed"))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // DELETE /api/v1/carts/me — Xóa toàn bộ giỏ hàng
    // ══════════════════════════════════════════════════════════════════

    @DeleteMapping("/me")
    @PreAuthorize("@permissionChecker.has('cart:manage')")
    public ApiResponse<Void> clearCart(Authentication authentication) {
        String email = authentication.getName();
        cartService.clearCart(email);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.cart.cleared"))
                .build();
    }
}
