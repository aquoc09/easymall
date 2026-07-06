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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;
}
