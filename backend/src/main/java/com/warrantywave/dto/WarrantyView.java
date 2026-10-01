package com.warrantywave.dto;

import java.time.LocalDate;

public record WarrantyView(Long id, Long vehicleId, String vehicle, String plan, LocalDate startDate, LocalDate endDate,
                           String status) { }