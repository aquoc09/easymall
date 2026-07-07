package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * NotificationEntity — Thông báo in-app cho người dùng.
 *
 * <p>Các loại thông báo (notification_type):
 * <ul>
 *   <li>{@code ORDER_UPDATE}  — trạng thái đơn hàng thay đổi</li>
 *   <li>{@code SYSTEM}        — thông báo hệ thống (bảo trì, cập nhật)</li>
 *   <li>{@code PROMOTION}     — khuyến mãi, voucher mới</li>
 *   <li>{@code REVIEW_REPLY}  — người bán trả lời đánh giá</li>
 * </ul>
 *
 * <p>TODO [BUSINESS LOGIC - cần implement sau]:
 * <ul>
 *   <li>NotificationService.send(): tạo record và push realtime qua WebSocket / FCM</li>
 *   <li>Hook ORDER_UPDATE: gửi khi order status thay đổi (PENDING→CONFIRMED→SHIPPING→COMPLETED)</li>
 *   <li>Hook REVIEW_REPLY: gửi khi admin/seller reply review của user</li>
 *   <li>NotificationController GET /api/v1/notifications: phân trang, filter is_read</li>
 *   <li>NotificationController PATCH /api/v1/notifications/{id}/read: đánh dấu đã đọc</li>
 *   <li>NotificationController PATCH /api/v1/notifications/read-all: đánh dấu đọc tất cả</li>
 * </ul>
 */
@Entity
@Table(name = "notifications",
    indexes = @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** ORDER_UPDATE / SYSTEM / PROMOTION / REVIEW_REPLY */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
