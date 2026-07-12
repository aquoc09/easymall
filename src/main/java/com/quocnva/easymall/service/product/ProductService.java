package com.quocnva.easymall.service.product;

import com.quocnva.easymall.dtos.request.product.ProductCreateRequest;
import com.quocnva.easymall.dtos.request.product.ProductUpdateRequest;
import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.dtos.response.product.ProductVariantResponse;

import com.quocnva.easymall.dtos.request.product.ProductFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

    ProductResponse getProductById(Long productId);

    ProductResponse getProductBySlug(String slug);

    Page<ProductResponse> getAllProducts(ProductFilterRequest filter, Pageable pageable);

    Page<ProductResponse> getPublicProducts(ProductFilterRequest filter, Pageable pageable);

    List<ProductVariantResponse> getVariantsByProductId(Long productId);

    void deleteProduct(Long productId);
}
