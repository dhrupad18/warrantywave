package com.warrantywave.repository;

import com.warrantywave.model.FinanceApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceApplicationRepository extends JpaRepository<FinanceApplication, Long> {
    List<FinanceApplication> findByCustomerEmailOrderByCreatedAtDesc(String email);
    List<FinanceApplication> findAllByOrderByCreatedAtDesc();
}