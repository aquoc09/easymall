package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * PaymentEntity — Lịch sử thanh toán, tách riêng khỏi OrderEntity.
 *
 * <p>Một đơn hàng có thể có nhiều payment record:
 * <ul>
 *   <li>Lần thử thanh toán đầu (FAILED)</li>
 *   <li>Retry lần 2 (PAID)</li>
 *   <li>Refund khi hủy đơn (REFUNDED)</li>
 * </ul>
 *
 * <p>TODO [BUSINESS LOGIC - cần implement sau]:
 * <ul>
 *   <li>PaymentService.initPayment(): tạo record PENDING khi user checkout</li>
 *   <li>PaymentService.handleVnpayCallback(): update status PAID/FAILED từ VNPAY IPN</li>
 *   <li>PaymentService.handleMomoCallback(): update status PAID/FAILED từ MoMo IPN</li>
 *   <li>PaymentService.refund(): tạo record REFUNDED khi order bị hủy sau khi đã thanh toán</li>
 *   <li>Hook vào OrderService: khi order COMPLETED → verify payment status = PAID</li>
 * </ul>
 */
@Entity
@Table(name = "payments",
    indexes = @Index(name = "idx_payments_order_id", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    /** 0: COD, 1: VNPAY, 2: MOMO */
    @Column(name = "payment_method", nullable = false)
    private Short paymentMethod;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Mã giao dịch từ cổng thanh toán (VNPAY txnRef, MoMo transId, v.v.) */
    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    /** PENDING → PAID / FAILED / REFUNDED */
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "PENDING";

    /** NULL cho đến khi cổng thanh toán confirm thành công */
    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
