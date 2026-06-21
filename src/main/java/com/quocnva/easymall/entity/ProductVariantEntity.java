package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    /**
     * variant_attributes: JSONB — tổ hợp thuộc tính của biến thể.
     * Ví dụ: {"color": "đỏ", "size": "M"}
     * Lưu dạng String JSON, parse trong service khi cần.
     */
    @Column(name = "variant_attributes", nullable = false, columnDefinition = "JSONB")
    private String variantAttributes;

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "variant_image", length = 500)
    private String variantImage;

    /**
     * stock_quantity >= 0 — CHECK constraint phía DB.
     */
    @Column(name = "stock_quantity", columnDefinition = "INT DEFAULT 0")
    private Integer stockQuantity;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive;

    /**
     * locked_stock >= 0 — số lượng đang bị giữ bởi đơn hàng chưa thanh toán.
     */
    @Column(name = "locked_stock", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer lockedStock;

    // ── Relationships ──────────────────────────────────────────────────

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InventoryTransactionEntity> inventoryTransactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (stockQuantity == null) stockQuantity = 0;
        if (isActive == null) isActive = true;
        if (lockedStock == null) lockedStock = 0;
    }
}
