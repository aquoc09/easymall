package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.UserBehaviorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehaviorEntity, Long> {
}
