package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email", unique = true))
public class UserAccount extends BaseEntity {
    @Column(nullable = false, length = 120)
    public String name;

    @Column(nullable = false, unique = true, length = 190)
    public String email;

    @Column(nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public UserRole role;

    protected UserAccount() { }

    public UserAccount(String name, String email, String passwordHash, UserRole role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}