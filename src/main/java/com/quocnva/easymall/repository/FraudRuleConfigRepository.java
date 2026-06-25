package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.FraudRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudRuleConfigRepository extends JpaRepository<FraudRuleConfigEntity, Integer> {
}
