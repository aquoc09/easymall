package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.product.ProductCreateRequest;
import com.quocnva.easymall.dtos.request.product.ProductUpdateRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.service.product.ProductService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ══════════════════════════════════════════════════════════════════
    // Public endpoints — product:read — no token required (see SecurityConfig)
    // ══════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/products/public — danh sách tất cả sản phẩm (storefront).
     */
    @GetMapping("/public")
    public ApiResponse<List<ProductResponse>> getAllProductsPublic() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getAllProducts())
                .build();
    }

    /**
     * GET /api/v1/products/public/{productId} — chi tiết sản phẩm theo ID.
     */
    @GetMapping("/public/{productId}")
    public ApiResponse<ProductResponse> getProductByIdPublic(@PathVariable Long productId) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductById(productId))
                .build();
    }

    /**
     * GET /api/v1/products/public/slug/{slug} — chi tiết sản phẩm theo slug (SEO-friendly).
     */
    @GetMapping("/public/slug/{slug}")
    public ApiResponse<ProductResponse> getProductBySlugPublic(@PathVariable String slug) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductBySlug(slug))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // Admin endpoints — require product permissions
    // ══════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/products — danh sách tất cả sản phẩm (admin view, bao gồm inactive).
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.has('product:read')")
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getAllProducts())
                .build();
    }

    /**
     * GET /api/v1/products/{productId} — chi tiết sản phẩm (admin).
     */
    @GetMapping("/{productId}")
    @PreAuthorize("@permissionChecker.has('product:read')")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductById(productId))
                .build();
    }

    /**
     * POST /api/v1/products — tạo sản phẩm mới kèm variants và images.
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.has('product:create')")
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(request))
                .message(Translator.toLocale("success.product.created"))
                .build();
    }

    /**
     * PUT /api/v1/products/{productId} — cập nhật sản phẩm (partial update — null fields bị bỏ qua).
     */
    @PutMapping("/{productId}")
    @PreAuthorize("@permissionChecker.has('product:update')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(productId, request))
                .message(Translator.toLocale("success.product.updated"))
                .build();
    }

    /**
     * DELETE /api/v1/products/{productId} — soft-delete sản phẩm (set inStock=false).
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("@permissionChecker.has('product:delete')")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.product.deleted"))
                .build();
    }
}
