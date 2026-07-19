package com.quocnva.easymall.controller;

import com.quocnva.easymall.util.Translator;

import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.review.WishlistResponse;
import com.quocnva.easymall.service.review.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    @PreAuthorize("@permissionChecker.has('wishlist:manage')")
    public ApiResponse<Map<String, Boolean>> toggle(
            @PathVariable Long productId,
            Authentication authentication) {
        boolean added = wishlistService.toggleWishlist(productId, authentication.getName());
        String message = added ? "Đã thêm vào danh sách yêu thích" : "Đã bỏ khỏi danh sách yêu thích";
        return ApiResponse.<Map<String, Boolean>>builder()
                .result(Map.of("inWishlist", added))
                .message(message)
                .build();
    }

    @GetMapping("/me")
    @PreAuthorize("@permissionChecker.has('wishlist:view')")
    public ApiResponse<Page<WishlistResponse>> getMyWishlist(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.<Page<WishlistResponse>>builder()
                .result(wishlistService.getMyWishlist(authentication.getName(), pageable))
                .build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("@permissionChecker.has('wishlist:manage')")
    public ApiResponse<Void> remove(
            @PathVariable Long productId,
            Authentication authentication) {
        wishlistService.removeFromWishlist(productId, authentication.getName());
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.wishlist.removed"))
                .build();
    }
}
