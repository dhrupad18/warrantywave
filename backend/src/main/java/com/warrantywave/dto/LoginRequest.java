package com.warrantywave.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.warrantywave.model.UserRole;

public record LoginRequest(@Email @NotBlank String email, @NotBlank String password, UserRole accountType) { }
