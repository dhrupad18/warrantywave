package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class FinanceContract extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    public FinanceApplication application;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal principal;

    @Column(nullable = false, precision = 6, scale = 3)
    public BigDecimal annualRate;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monthlyEmi;

    @Column(nullable = false)
    public int tenureMonths;

    public LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public ContractStatus status = ContractStatus.ACTIVE;

    public FinanceContract() { }
}