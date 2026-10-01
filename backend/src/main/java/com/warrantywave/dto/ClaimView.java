package com.warrantywave.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClaimView(Long id, Long warrantyId, String vehicle, String part, String description, BigDecimal costEstimate,
                        String status, String validationNote, String reviewNote, LocalDateTime createdAt) { }