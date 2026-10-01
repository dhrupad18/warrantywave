package com.warrantywave.repository;

import com.warrantywave.model.FinanceContract;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceContractRepository extends JpaRepository<FinanceContract, Long> {
    Optional<FinanceContract> findByApplicationId(Long applicationId);
}