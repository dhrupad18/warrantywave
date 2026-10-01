package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_assessments")
public class CreditAssessment extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    public FinanceApplication application;

    @Column(nullable = false)
    public int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    public RiskBand riskBand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    public CreditDecision decision;

    @Column(nullable = false)
    public double annualRate;

    @Column(nullable = false, length = 1500)
    public String reasons;

    public CreditAssessment() { }
}