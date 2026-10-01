package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles", indexes = @Index(name = "idx_vehicles_owner", columnList = "owner_id"))
public class Vehicle extends BaseEntity {
    @Column(nullable = false, unique = true, length = 17)
    public String vin;

    @Column(nullable = false, length = 80)
    public String make;

    @Column(nullable = false, length = 80)
    public String model;

    @Column(nullable = false)
    public int year;

    @Column(nullable = false)
    public int currentMileage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    public UserAccount owner;

    public Vehicle() { }
}