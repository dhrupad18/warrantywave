package com.warrantywave.repository;

import com.warrantywave.model.WarrantyClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    List<WarrantyClaim> findByWarrantyVehicleOwnerEmailOrderByCreatedAtDesc(String email);
    List<WarrantyClaim> findAllByOrderByCreatedAtDesc();
}