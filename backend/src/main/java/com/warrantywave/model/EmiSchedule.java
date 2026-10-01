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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "emi_schedule", uniqueConstraints = @UniqueConstraint(name = "uk_emi_contract_installment", columnNames = {"contract_id", "installment_no"}), indexes = @Index(name = "idx_emi_status_due_date", columnList = "status,due_date"))
public class EmiSchedule extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    public FinanceContract contract;

    @Column(name = "installment_no", nullable = false)
    public int installmentNo;

    @Column(nullable = false)
    public LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal principalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal interestAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal emiAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal lateFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public EmiStatus status = EmiStatus.PENDING;

    public LocalDate paidAt;

    public EmiSchedule() { }
}