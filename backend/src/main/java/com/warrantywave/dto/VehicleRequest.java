package com.warrantywave.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(@NotBlank @Pattern(regexp = "[A-HJ-NPR-Z0-9a-hj-npr-z]{17}") String vin,
                             @NotBlank @Size(max = 80) String make,
                             @NotBlank @Size(max = 80) String model,
                             @Min(1950) int year,
                             @Min(0) int currentMileage) { }