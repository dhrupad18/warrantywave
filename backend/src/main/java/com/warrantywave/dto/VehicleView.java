package com.warrantywave.dto;

public record VehicleView(Long id, String vin, String make, String model, int year, int currentMileage) { }