package com.warrantywave.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ClaimRequest(@NotNull Long warrantyId, @NotBlank @Size(max = 100) String part,
                           @NotBlank @Size(max = 2000) String description,
                           @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal costEstimate) { }