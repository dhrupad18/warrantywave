package com.warrantywave.service;

import com.warrantywave.model.UserAccount;
import com.warrantywave.WarrantyWaveProperties;
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
        this.signingKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
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