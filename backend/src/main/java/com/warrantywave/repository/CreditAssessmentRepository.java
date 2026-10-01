package com.warrantywave.repository;

import com.warrantywave.model.CreditAssessment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAssessmentRepository extends JpaRepository<CreditAssessment, Long> {
    Optional<CreditAssessment> findByApplicationId(Long applicationId);
}