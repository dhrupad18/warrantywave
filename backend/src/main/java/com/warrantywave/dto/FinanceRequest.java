package com.warrantywave.dto;

import com.warrantywave.model.FinanceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FinanceRequest(@NotNull Long vehicleId, @NotNull FinanceType type,
                             @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal vehiclePrice,
                             @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal downPayment,
                             @Min(1) @Max(60) int tenureMonths,
                             @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monthlyIncome,
                             @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal existingEmi,
                             @Min(300) @Max(850) int bureauScore, boolean incomeStable) { }