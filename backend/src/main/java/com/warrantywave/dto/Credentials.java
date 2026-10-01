package com.warrantywave.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Credentials(@NotBlank @Size(max = 120) String name,
                         @Email @NotBlank @Size(max = 190) String email,
                         @NotBlank @Size(min = 10, max = 200) String password) { }