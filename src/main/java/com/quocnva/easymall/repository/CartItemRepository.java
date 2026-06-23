package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    Optional<CartItemEntity> findByCart_CartIdAndVariant_VariantId(Long cartId, Long variantId);

    void deleteByCart_CartId(Long cartId);

    /** Bulk load cart items khi checkout */
    List<CartItemEntity> findAllByCartItemIdIn(List<Long> cartItemIds);

    /** Xóa cart items sau khi checkout thành công */
    void deleteAllByCartItemIdIn(List<Long> cartItemIds);
}
