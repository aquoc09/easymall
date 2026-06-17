package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {

    /** Lookup by the RT JWT string — used in refresh() to find the existing token row. */
    Optional<TokenEntity> findByRefreshToken(String refreshToken);

    /** Deletes the RT row on logout — this is how RT revocation works (no is_revoked flag). */
    void deleteByRefreshToken(String refreshToken);
}
