package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.ContactMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, Long> {

    Page<ContactMessageEntity> findByStatus(String status, Pageable pageable);

    Page<ContactMessageEntity> findByUser_UserId(Long userId, Pageable pageable);
}
