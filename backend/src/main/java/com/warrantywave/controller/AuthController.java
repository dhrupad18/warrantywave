package com.warrantywave.controller;

import com.warrantywave.dto.Credentials;
import com.warrantywave.dto.LoginRequest;
import com.warrantywave.model.UserAccount;
import com.warrantywave.model.UserRole;
import com.warrantywave.repository.UserAccountRepository;
import com.warrantywave.service.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody Credentials credentials) {
        String email = credentials.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        UserAccount user = users.save(new UserAccount(credentials.name().trim(), email,
            passwordEncoder.encode(credentials.password()), UserRole.CUSTOMER));
        return session(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest credentials) {
        UserAccount user = users.findByEmail(credentials.email().trim().toLowerCase(Locale.ROOT))
            .filter(account -> passwordEncoder.matches(credentials.password(), account.passwordHash))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return session(user);
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        UserAccount user = users.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Map.of("id", user.id, "name", user.name, "email", user.email, "role", user.role.name());
    }

    private Map<String, Object> session(UserAccount user) {
        return Map.of("token", jwtService.createToken(user), "id", user.id,
            "name", user.name, "email", user.email, "role", user.role.name());
    }

}