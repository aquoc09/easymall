package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id")
    private Long orderDetailId;

    @Column(name = "num_of_product", nullable = false)
    private Integer numOfProduct;

    @Column(name = "order_detail_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal orderDetailPrice;

    @Column(name = "total_money", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalMoney;

    @Builder.Default
    @Column(name = "item_status", nullable = false, length = 30)
    private String itemStatus = "NORMAL";

    @Column(name = "product_name")
    private String productName;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "variant_attributes", columnDefinition = "JSONB")
    private java.util.Map<String, String> variantAttributes;

    @Column(name = "sku_code", length = 50)
    private String skuCode;

    @Column(name = "variant_image", length = 500)
    private String variantImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private ProductVariantEntity variant;
}
