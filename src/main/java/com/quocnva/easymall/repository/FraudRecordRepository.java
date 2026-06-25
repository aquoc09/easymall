package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.FraudRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRecordRepository extends JpaRepository<FraudRecordEntity, Long> {
}
