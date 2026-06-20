package com.quocnva.easymall.service.category.impl;

import com.quocnva.easymall.dtos.request.category.CategoryCreateRequest;
import com.quocnva.easymall.dtos.request.category.CategoryUpdateRequest;
import com.quocnva.easymall.dtos.response.category.CategoryResponse;
import com.quocnva.easymall.entity.CategoryEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.CategoryMapper;
import com.quocnva.easymall.repository.CategoryRepository;
import com.quocnva.easymall.service.category.CategoryService;
import com.quocnva.easymall.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree(boolean isPublic) {
        List<CategoryEntity> entities;
        if (isPublic) {
            entities = categoryRepository.findByCategoryStatusOrderByLevelAscDisplayOrderAsc(1);
        } else {
            entities = categoryRepository.findAllByOrderByLevelAscDisplayOrderAsc();
        }

        Map<Long, CategoryResponse> categoryMap = new HashMap<>();
        List<CategoryResponse> rootCategories = new ArrayList<>();

        for (CategoryEntity entity : entities) {
            CategoryResponse response = categoryMapper.toResponse(entity);
            categoryMap.put(response.getCategoryId(), response);

            if (response.getParentId() == null) {
                rootCategories.add(response);
            } else {
                CategoryResponse parent = categoryMap.get(response.getParentId());
                if (parent != null) {
                    parent.getChildren().add(response);
                } else {
                    // Trưởng hợp dữ liệu lỗi hoặc danh mục cha đã bị xoá mà con vẫn còn (dù DB có foreign key set null)
                    // Ta tạm add vào root để không bị mất dữ liệu hiển thị.
                    rootCategories.add(response);
                }
            }
        }
        return rootCategories;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String categoryCode = SlugUtils.toSlug(request.getCategoryName());
        
        if (categoryRepository.existsByCategoryCode(categoryCode)) {
            throw new AppException(ErrorCode.CATEGORY_CODE_ALREADY_EXISTS);
        }

        CategoryEntity entity = categoryMapper.toEntity(request);
        entity.setCategoryCode(categoryCode);
        entity.setCategoryStatus(1); // Mặc định hiển thị

        if (request.getParentId() == null) {
            entity.setLevel(1);
        } else {
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARENT_CATEGORY_NOT_FOUND));
            
            int newLevel = (parent.getLevel() != null ? parent.getLevel() : 1) + 1;
            if (newLevel > 3) {
                throw new AppException(ErrorCode.MAX_LEVEL_EXCEEDED);
            }
            entity.setLevel(newLevel);
        }

        if (entity.getDisplayOrder() == null) {
            entity.setDisplayOrder(0);
        }
        if (entity.getCategoryType() == null || entity.getCategoryType().isEmpty()) {
            entity.setCategoryType("STANDARD");
        }

        entity = categoryRepository.save(entity);
        return categoryMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        CategoryEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        boolean wasActive = entity.getCategoryStatus() != null && entity.getCategoryStatus() == 1;
        boolean isNowHidden = request.getCategoryStatus() != null && request.getCategoryStatus() == 0;

        categoryMapper.updateEntityFromRequest(request, entity);
        // We DO NOT update categoryCode to preserve SEO links as per business logic requirement.

        entity = categoryRepository.save(entity);

        // Cascade Toggle children if status changed from 1 to 0
        if (wasActive && isNowHidden) {
            cascadeHideChildren(categoryId);
        }

        return categoryMapper.toResponse(entity);
    }

    private void cascadeHideChildren(Long parentId) {
        List<CategoryEntity> allCategories = categoryRepository.findAllByOrderByLevelAscDisplayOrderAsc();
        Map<Long, List<CategoryEntity>> childrenMap = new HashMap<>();
        for (CategoryEntity c : allCategories) {
            if (c.getParentId() != null) {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        
        List<CategoryEntity> toHide = new ArrayList<>();
        gatherDescendantsToHide(parentId, childrenMap, toHide);
        
        if (!toHide.isEmpty()) {
            for (CategoryEntity child : toHide) {
                child.setCategoryStatus(0);
            }
            categoryRepository.saveAll(toHide);
        }
    }

    private void gatherDescendantsToHide(Long parentId, Map<Long, List<CategoryEntity>> childrenMap, List<CategoryEntity> toHide) {
        List<CategoryEntity> children = childrenMap.get(parentId);
        if (children != null) {
            for (CategoryEntity child : children) {
                if (child.getCategoryStatus() != null && child.getCategoryStatus() != 0) {
                    toHide.add(child);
                    gatherDescendantsToHide(child.getCategoryId(), childrenMap, toHide);
                }
            }
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        CategoryEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        long childrenCount = categoryRepository.countByParentId(categoryId);
        if (childrenCount > 0) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        // Skeleton for CategoryInUseException (checking products)
        boolean hasProducts = checkCategoryHasProducts(categoryId);
        if (hasProducts) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }

        categoryRepository.delete(entity);
    }

    private boolean checkCategoryHasProducts(Long categoryId) {
        // TODO: Implement actual product check when Product module is available.
        return false;
    }
}
