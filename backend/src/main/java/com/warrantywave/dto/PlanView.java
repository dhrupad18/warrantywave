package com.warrantywave.dto;

import java.math.BigDecimal;

public record PlanView(Long id, String name, String coveredParts, int durationMonths, int kmLimit, BigDecimal price) { }