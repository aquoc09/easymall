package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.InventoryTransactionEntity;
import com.quocnva.easymall.enums.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, Long> {

    List<InventoryTransactionEntity> findAllByVariantVariantIdOrderByCreatedAtDesc(Long variantId);

    List<InventoryTransactionEntity> findAllByTransactionType(InventoryTransactionType transactionType);
}
