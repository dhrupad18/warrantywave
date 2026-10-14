package com.warrantywave.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.warrantywave.WarrantyWaveProperties;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    @Test
    void rejectsMissingSigningSecretAtStartup() {
        WarrantyWaveProperties properties = new WarrantyWaveProperties();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new JwtService(properties));

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void rejectsSigningSecretShorterThan256Bits() {
        WarrantyWaveProperties properties = new WarrantyWaveProperties();
        properties.getJwt().setSecret("too-short");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new JwtService(properties));

        assertTrue(exception.getMessage().contains("32 UTF-8 bytes"));
    }
}