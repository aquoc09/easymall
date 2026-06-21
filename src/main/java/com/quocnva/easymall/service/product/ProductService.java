package com.quocnva.easymall.service.product;

import com.quocnva.easymall.dtos.request.product.ProductCreateRequest;
import com.quocnva.easymall.dtos.request.product.ProductUpdateRequest;
import com.quocnva.easymall.dtos.response.product.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

    ProductResponse getProductById(Long productId);

    ProductResponse getProductBySlug(String slug);

    List<ProductResponse> getAllProducts();

    void deleteProduct(Long productId);
}
