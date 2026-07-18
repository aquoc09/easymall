package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.RiskRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskRuleConfigRepository extends JpaRepository<RiskRuleConfigEntity, String> {
    List<RiskRuleConfigEntity> findByIsActiveTrue();
}
