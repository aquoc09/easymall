package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.WishlistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {

    Optional<WishlistEntity> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    Page<WishlistEntity> findByUser_UserId(Long userId, Pageable pageable);

    void deleteByUser_UserIdAndProduct_ProductId(Long userId, Long productId);
}
