package com.quocnva.easymall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    /**
     * Stores the full Refresh Token JWT string (VARCHAR 1000).
     * Access Tokens are NEVER persisted — they are blacklisted in Redis on logout.
     * is_revoked is vestigial; RT revocation = row deletion.
     */
    @Column(name = "refresh_token", nullable = false, length = 300)
    private String refreshToken;

    @Builder.Default
    @Column(name = "is_revoked")
    private Boolean isRevoked = false;

    /** RT expiry timestamp — NOT NULL, kiểm tra trước khi cho phép rotation trong refresh(). */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
