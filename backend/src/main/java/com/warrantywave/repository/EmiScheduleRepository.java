package com.warrantywave.repository;

import com.warrantywave.model.EmiSchedule;
import com.warrantywave.model.EmiStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {
    List<EmiSchedule> findByContractApplicationCustomerEmailOrderByDueDateAsc(String email);
    List<EmiSchedule> findByStatusInAndDueDateBefore(List<EmiStatus> statuses, LocalDate date);
}