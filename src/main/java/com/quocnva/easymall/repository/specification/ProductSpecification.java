package com.quocnva.easymall.repository.specification;

import com.quocnva.easymall.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<ProductEntity> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), likePattern),
                    cb.like(cb.lower(root.get("productSlug")), likePattern)
            );
        };
    }

    public static Specification<ProductEntity> hasCategory(List<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return null;
            }
            return root.get("categoryId").in(categoryIds);
        };
    }

    public static Specification<ProductEntity> isInStock(Boolean inStock) {
        return (root, query, cb) -> {
            if (inStock == null) {
                return null;
            }
            return cb.equal(root.get("inStock"), inStock);
        };
    }

    public static Specification<ProductEntity> isPopular(Boolean inPopular) {
        return (root, query, cb) -> {
            if (inPopular == null) {
                return null;
            }
            return cb.equal(root.get("inPopular"), inPopular);
        };
    }

    public static Specification<ProductEntity> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                // If the user wants minPrice, the product's maxPrice must be >= user's minPrice
                // For safety, we just ensure product maxPrice is not less than requested minPrice
                predicates.add(cb.greaterThanOrEqualTo(root.get("maxPrice"), minPrice));
            }
            if (maxPrice != null) {
                // The product's minPrice must be <= user's maxPrice
                predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), maxPrice));
            }
            if (predicates.isEmpty()) return null;
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ProductEntity> hasRatingGreaterThanEqual(BigDecimal minRating) {
        return (root, query, cb) -> {
            if (minRating == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating);
        };
    }

    public static Specification<ProductEntity> hasTargetGender(Short targetGender) {
        return (root, query, cb) -> {
            if (targetGender == null) {
                return null;
            }
            // 2 is unisex, if targetGender is 0 (Female) or 1 (Male), we might also include 2 (Unisex) products?
            // Usually, yes. A unisex product applies to both.
            // If the user explicitly asks for female, they get female + unisex.
            if (targetGender == 0 || targetGender == 1) {
                return cb.or(
                        cb.equal(root.get("targetGender"), targetGender),
                        cb.equal(root.get("targetGender"), (short) 2)
                );
            }
            // If targetGender == 2, just return unisex
            return cb.equal(root.get("targetGender"), targetGender);
        };
    }
}
