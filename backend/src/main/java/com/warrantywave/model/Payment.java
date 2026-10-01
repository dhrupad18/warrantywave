package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emi_id", nullable = false, unique = true)
    public EmiSchedule emi;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false)
    public LocalDateTime paidAt;

    public Payment() { }
}