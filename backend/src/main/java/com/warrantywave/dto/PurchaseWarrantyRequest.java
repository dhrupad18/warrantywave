package com.warrantywave.dto;

import jakarta.validation.constraints.NotNull;

public record PurchaseWarrantyRequest(@NotNull Long vehicleId, @NotNull Long planId) { }