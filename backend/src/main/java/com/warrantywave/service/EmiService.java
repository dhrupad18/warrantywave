package com.warrantywave.service;

import com.warrantywave.model.EmiSchedule;
import com.warrantywave.model.EmiStatus;
import com.warrantywave.WarrantyWaveProperties;
import com.warrantywave.repository.EmiScheduleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmiService {
    private final EmiScheduleRepository schedules;
    private final WarrantyWaveProperties properties;

    public EmiService(EmiScheduleRepository schedules, WarrantyWaveProperties properties) {
        this.schedules = schedules;
        this.properties = properties;
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void applyLateFees() {
        List<EmiSchedule> overdue = schedules.findByStatusInAndDueDateBefore(
            List.of(EmiStatus.PENDING, EmiStatus.OVERDUE), LocalDate.now());
        BigDecimal rate = BigDecimal.valueOf(properties.getLateFeePercent()).movePointLeft(2);
        for (EmiSchedule emi : overdue) {
            if (emi.lateFee == null || emi.lateFee.signum() == 0) {
                emi.lateFee = emi.emiAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }
            emi.status = EmiStatus.OVERDUE;
        }
    }
}