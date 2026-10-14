package com.warrantywave.repository;

import com.warrantywave.model.VehicleWarranty;
import com.warrantywave.model.WarrantyStatus;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleWarrantyRepository extends JpaRepository<VehicleWarranty, Long> {
    List<VehicleWarranty> findByVehicleOwnerEmailOrderByIdDesc(String email);
    java.util.Optional<VehicleWarranty> findByIdAndVehicleOwnerEmail(Long id, String email);
    boolean existsByVehicle_IdAndStatusAndEndDateGreaterThanEqual(Long vehicleId, WarrantyStatus status, LocalDate endDate);
}
