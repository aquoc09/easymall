package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    boolean existsByCategoryCode(String categoryCode);

    boolean existsByCategoryCodeAndCategoryIdNot(String categoryCode, Long categoryId);

    long countByParentId(Long parentId);
    
    List<CategoryEntity> findByCategoryStatusOrderByLevelAscDisplayOrderAsc(Short categoryStatus);
    
    List<CategoryEntity> findAllByOrderByLevelAscDisplayOrderAsc();

    Optional<CategoryEntity> findByCategoryCode(String categoryCode);

    List<CategoryEntity> findByParentId(Long parentId);

}
