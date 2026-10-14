package com.warrantywave.repository;

import com.warrantywave.model.Vehicle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerEmailOrderByIdDesc(String email);
    java.util.Optional<Vehicle> findByIdAndOwnerEmail(Long id, String email);
    boolean existsByVin(String vin);
}
