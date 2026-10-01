package com.warrantywave.service;

import com.warrantywave.model.ContractStatus;
import com.warrantywave.model.EmiSchedule;
import com.warrantywave.model.EmiStatus;
import com.warrantywave.model.FinanceApplication;
import com.warrantywave.model.FinanceContract;
import com.warrantywave.repository.EmiScheduleRepository;
import com.warrantywave.repository.FinanceContractRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceContractService {
    private final FinanceContractRepository contracts;
    private final EmiScheduleRepository schedules;

    public FinanceContractService(FinanceContractRepository contracts, EmiScheduleRepository schedules) {
        this.contracts = contracts;
        this.schedules = schedules;
    }

    @Transactional
    public void createContract(FinanceApplication application, double annualRate) {
        BigDecimal principal = application.requestedAmount;
        BigDecimal monthlyRate = BigDecimal.valueOf(annualRate).movePointLeft(2)
            .divide(BigDecimal.valueOf(12), 12, RoundingMode.HALF_UP);
        double rate = monthlyRate.doubleValue();
        double factor = Math.pow(1 + rate, application.tenureMonths);
        BigDecimal monthlyEmi = BigDecimal.valueOf(rate == 0 ? principal.doubleValue() / application.tenureMonths
            : principal.doubleValue() * rate * factor / (factor - 1)).setScale(2, RoundingMode.HALF_UP);
        FinanceContract contract = new FinanceContract();
        contract.application = application;
        contract.principal = principal;
        contract.annualRate = BigDecimal.valueOf(annualRate).setScale(3, RoundingMode.HALF_UP);
        contract.monthlyEmi = monthlyEmi;
        contract.tenureMonths = application.tenureMonths;
        contract.startDate = LocalDate.now();
        contract.status = ContractStatus.ACTIVE;
        contract = contracts.save(contract);
        BigDecimal balance = principal;
        for (int month = 1; month <= application.tenureMonths; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal due = month == application.tenureMonths ? balance.add(interest) : monthlyEmi;
            BigDecimal principalPart = due.subtract(interest).min(balance);
            EmiSchedule emi = new EmiSchedule();
            emi.contract = contract;
            emi.installmentNo = month;
            emi.dueDate = contract.startDate.plusMonths(month);
            emi.principalAmount = principalPart;
            emi.interestAmount = interest;
            emi.emiAmount = due;
            emi.lateFee = BigDecimal.ZERO;
            emi.status = EmiStatus.PENDING;
            schedules.save(emi);
            balance = balance.subtract(principalPart).max(BigDecimal.ZERO);
        }
    }
}