package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.SliderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SliderRepository extends JpaRepository<SliderEntity, Long> {
    
    List<SliderEntity> findByIsActiveTrueOrderByDisplayOrderAsc();
    
}
