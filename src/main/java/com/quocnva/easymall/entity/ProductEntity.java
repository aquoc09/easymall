package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_slug", nullable = false, unique = true, length = 100)
    private String productSlug;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "product_description", columnDefinition = "text")
    private String productDescription;

    @Column(name = "in_popular")
    private Boolean inPopular;

    @Column(name = "in_stock")
    private Boolean inStock;

    /**
     * target_gender: 0=Nữ, 1=Nam, 2=Unisex.
     * Dùng SMALLINT, không dùng enum tránh migration phức tạp.
     */
    @Column(name = "target_gender")
    private Short targetGender;

    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    /**
     * options_config: JSONB — cấu hình các option của sản phẩm (ví dụ: màu, size).
     * Lưu dưới dạng String JSON, convert thủ công qua service/mapper.
     */
    @Column(name = "options_config", columnDefinition = "jsonb")
    private String optionsConfig;

    /**
     * product_tags: JSONB — danh sách tag. Ví dụ: ["vintage", "oversize"].
     * DEFAULT '[]'::jsonb phía DB.
     */
    @Column(name = "product_tags", columnDefinition = "jsonb")
    private String productTags;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "length_m", precision = 5, scale = 2)
    private BigDecimal lengthM;

    @Column(name = "width_m", precision = 5, scale = 2)
    private BigDecimal widthM;

    @Column(name = "height_m", precision = 5, scale = 2)
    private BigDecimal heightM;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * search_vector: TSVECTOR — được DB trigger tự động cập nhật, không set từ Java.
     * insertable=false, updatable=false để Hibernate không ghi vào cột này.
     */
    @Column(name = "search_vector", insertable = false, updatable = false, columnDefinition = "tsvector")
    private String searchVector;

    // ── Relationships ──────────────────────────────────────────────────

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariantEntity> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImageEntity> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (inPopular == null) inPopular = false;
        if (inStock == null) inStock = true;
        if (targetGender == null) targetGender = (short) 2;
        if (maxOrderQuantity == null) maxOrderQuantity = 0;
    }
}
