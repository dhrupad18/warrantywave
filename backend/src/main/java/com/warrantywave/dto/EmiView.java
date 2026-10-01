package com.warrantywave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmiView(Long id, Long applicationId, int installmentNo, LocalDate dueDate, BigDecimal amount,
                      BigDecimal lateFee, String status, LocalDate paidAt) { }