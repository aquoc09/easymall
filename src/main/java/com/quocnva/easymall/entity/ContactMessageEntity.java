package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * ContactMessageEntity — Lưu trữ tin nhắn liên hệ từ khách hàng.
 *
 * <p>Trạng thái (status):
 * <ul>
 *   <li>{@code PENDING}  — Chờ xử lý</li>
 *   <li>{@code RESOLVED} — Đã giải quyết xong</li>
 *   <li>{@code REJECTED} — Bị từ chối (spam, không hợp lệ)</li>
 * </ul>
 */
@Entity
@Table(name = "contact_messages",
    indexes = @Index(name = "idx_contact_dashboard", columnList = "status, created_at DESC"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(name = "subject", length = 200, nullable = false)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
