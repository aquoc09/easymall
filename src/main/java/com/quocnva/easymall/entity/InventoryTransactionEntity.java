package com.quocnva.easymall.entity;

import com.quocnva.easymall.enums.InventoryTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    /**
     * Giá trị âm = xuất kho, dương = nhập kho.
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /**
     * Lưu dưới dạng String → DB column VARCHAR(50).
     * Sử dụng @Enumerated(EnumType.STRING) để map tự động.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private InventoryTransactionType transactionType;

    /**
     * Chứa order_id, return_id hoặc phiếu nhập kho tương ứng. Nullable.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * TIMESTAMPTZ — sử dụng OffsetDateTime để giữ timezone info.
     */
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
