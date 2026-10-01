package com.warrantywave.repository;

import com.warrantywave.model.WarrantyPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarrantyPlanRepository extends JpaRepository<WarrantyPlan, Long> {
    List<WarrantyPlan> findByActiveTrueOrderByPriceAsc();
}