package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /** Nullable: OAuth-only user không cần password. Constraint chk_users_password đảm bảo tính hợp lệ ở tầng DB. */
    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** 0: Nữ, 1: Nam, 2: Khác */
    @Column(name = "gender")
    private Short gender;

    @Column(name = "facebook_account_id", length = 255)
    private String facebookAccountId;

    @Column(name = "google_account_id", length = 255)
    private String googleAccountId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "avatar", length = 500)
    private String avatar;

    /** Thời điểm user xác thực email. NULL = chưa xác thực. */
    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    /** Thời điểm đăng nhập gần nhất — dùng cho audit và security. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private RoleEntity role;
}
