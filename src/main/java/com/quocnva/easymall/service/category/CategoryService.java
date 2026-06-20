package com.quocnva.easymall.service.category;

import com.quocnva.easymall.dtos.request.category.CategoryCreateRequest;
import com.quocnva.easymall.dtos.request.category.CategoryUpdateRequest;
import com.quocnva.easymall.dtos.response.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    
    List<CategoryResponse> getCategoryTree(boolean isPublic);
    
    CategoryResponse createCategory(CategoryCreateRequest request);
    
    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request);
    
    void deleteCategory(Long categoryId);
}
