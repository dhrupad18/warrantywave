package com.warrantywave.security;

import com.warrantywave.WarrantyWaveProperties;
import com.warrantywave.model.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final WarrantyWaveProperties properties;
    private final SecretKey signingKey;

    public JwtService(WarrantyWaveProperties properties) {
        this.properties = properties;
        String configuredSecret = properties.getJwt().getSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public String createToken(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.email)
            .claim("role", user.role.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(properties.getJwt().getExpiryMinutes() * 60)))
            .signWith(signingKey)
            .compact();
    }

    public String readSubject(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload().getSubject();
    }
}