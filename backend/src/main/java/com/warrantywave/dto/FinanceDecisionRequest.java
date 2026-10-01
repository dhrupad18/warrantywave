package com.warrantywave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinanceDecisionRequest(boolean approved, @NotBlank @Size(max = 1000) String note) { }