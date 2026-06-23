package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findAllByUserUserId(Long userId);

    Optional<AddressEntity> findByUserUserIdAndIsDefaultTrue(Long userId);

    /** Validate address ownership khi checkout */
    Optional<AddressEntity> findByAddressIdAndUser_UserId(Long addressId, Long userId);
}
