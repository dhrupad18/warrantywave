package com.warrantywave.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.warrantywave.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByEmiId(Long emiId);
}