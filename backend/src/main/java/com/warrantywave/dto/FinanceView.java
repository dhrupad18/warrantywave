package com.warrantywave.dto;

import java.math.BigDecimal;

public record FinanceView(Long id, String type, String vehicle, BigDecimal requestedAmount, int tenureMonths, String status,
                          String decisionNote, Integer score, String riskBand, Double annualRate, BigDecimal monthlyEmi) { }