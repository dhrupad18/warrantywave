package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "finance_applications", indexes = @Index(name = "idx_finance_customer_status", columnList = "customer_id,status"))
public class FinanceApplication extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    public UserAccount customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    public Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public FinanceType type;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal requestedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal vehiclePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal downPayment;

    @Column(nullable = false)
    public int tenureMonths;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monthlyIncome;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal existingEmi;

    @Column(nullable = false)
    public int bureauScore;

    @Column(nullable = false)
    public boolean incomeStable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(length = 1000)
    public String decisionNote;

    public FinanceApplication() { }
}