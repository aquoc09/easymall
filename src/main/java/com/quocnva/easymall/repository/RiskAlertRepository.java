package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.RiskAlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlertEntity, Long> {
    Page<RiskAlertEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<RiskAlertEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<RiskAlertEntity> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
