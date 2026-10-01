package com.warrantywave.controller;

import com.warrantywave.*;
import com.warrantywave.dto.*;
import com.warrantywave.model.*;
import com.warrantywave.repository.*;
import com.warrantywave.service.FinanceContractService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class WorkflowController {
    private final UserAccountRepository users;
    private final VehicleRepository vehicles;
    private final WarrantyPlanRepository plans;
    private final VehicleWarrantyRepository warranties;
    private final WarrantyClaimRepository claims;
    private final FinanceApplicationRepository applications;
    private final CreditAssessmentRepository assessments;
    private final FinanceContractRepository contracts;
    private final EmiScheduleRepository schedules;
    private final PaymentRepository payments;
    private final WarrantyWaveProperties properties;
    private final FinanceContractService financeContractService;

    public WorkflowController(UserAccountRepository users, VehicleRepository vehicles, WarrantyPlanRepository plans,
                              VehicleWarrantyRepository warranties, WarrantyClaimRepository claims,
                              FinanceApplicationRepository applications, CreditAssessmentRepository assessments,
                              FinanceContractRepository contracts, EmiScheduleRepository schedules,
                              PaymentRepository payments, WarrantyWaveProperties properties,
                              FinanceContractService financeContractService) {
        this.users = users;
        this.vehicles = vehicles;
        this.plans = plans;
        this.warranties = warranties;
        this.claims = claims;
        this.applications = applications;
        this.assessments = assessments;
        this.contracts = contracts;
        this.schedules = schedules;
        this.payments = payments;
        this.properties = properties;
        this.financeContractService = financeContractService;
    }

    @GetMapping("/vehicles")
    @Transactional(readOnly = true)
    public List<VehicleView> vehicles(Authentication auth) {
        return vehicles.findByOwnerEmailOrderByIdDesc(auth.getName()).stream().map(WorkflowController::vehicleView).toList();
    }

    @PostMapping("/vehicles")
    @Transactional
    public VehicleView addVehicle(Authentication auth, @Valid @RequestBody VehicleRequest request) {
        if (request.year() > LocalDate.now().getYear() + 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle year cannot be more than one year in the future");
        String vin = request.vin().trim().toUpperCase(Locale.ROOT);
        if (vehicles.existsByVin(vin)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A vehicle with this VIN is already registered");
        Vehicle vehicle = new Vehicle();
        vehicle.owner = user(auth.getName());
        vehicle.vin = vin;
        vehicle.make = request.make().trim();
        vehicle.model = request.model().trim();
        vehicle.year = request.year();
        vehicle.currentMileage = request.currentMileage();
        return vehicleView(vehicles.save(vehicle));
    }

    @GetMapping("/warranty/plans")
    @Transactional(readOnly = true)
    public List<PlanView> plans() {
        return plans.findByActiveTrueOrderByPriceAsc().stream()
            .map(plan -> new PlanView(plan.id, plan.name, plan.coveredParts, plan.durationMonths, plan.kmLimit, plan.price)).toList();
    }

    @GetMapping("/warranties")
    @Transactional(readOnly = true)
    public List<WarrantyView> warranties(Authentication auth) {
        return warranties.findByVehicleOwnerEmailOrderByIdDesc(auth.getName()).stream().map(WorkflowController::warrantyView).toList();
    }

    @PostMapping("/warranties")
    @Transactional
    public WarrantyView purchaseWarranty(Authentication auth, @Valid @RequestBody PurchaseWarrantyRequest request) {
        Vehicle vehicle = ownedVehicle(request.vehicleId(), auth.getName());
        if (warranties.existsByVehicle_IdAndStatusAndEndDateGreaterThanEqual(vehicle.id, WarrantyStatus.ACTIVE, LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This vehicle already has active warranty coverage");
        WarrantyPlan plan = plans.findById(request.planId()).filter(item -> item.active)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warranty plan not found"));
        VehicleWarranty warranty = new VehicleWarranty();
        warranty.vehicle = vehicle;
        warranty.plan = plan;
        warranty.startDate = LocalDate.now();
        warranty.endDate = warranty.startDate.plusMonths(plan.durationMonths);
        warranty.status = WarrantyStatus.ACTIVE;
        return warrantyView(warranties.save(warranty));
    }

    @GetMapping("/claims")
    @Transactional(readOnly = true)
    public List<ClaimView> claims(Authentication auth) {
        return hasRole(auth, UserRole.ADJUSTER, UserRole.ADMIN) ? claims.findAllByOrderByCreatedAtDesc().stream().map(WorkflowController::claimView).toList()
            : claims.findByWarrantyVehicleOwnerEmailOrderByCreatedAtDesc(auth.getName()).stream().map(WorkflowController::claimView).toList();
    }

    @PostMapping("/claims")
    @Transactional
    public ClaimView submitClaim(Authentication auth, @Valid @RequestBody ClaimRequest request) {
        VehicleWarranty warranty = warranties.findById(request.warrantyId()).filter(item -> item.vehicle.owner.email.equals(auth.getName()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warranty not found"));
        LocalDate today = LocalDate.now();
        boolean inTerm = warranty.status == WarrantyStatus.ACTIVE && !today.isBefore(warranty.startDate) && !today.isAfter(warranty.endDate);
        boolean withinMileage = warranty.vehicle.currentMileage <= warranty.plan.kmLimit;
        boolean coveredPart = List.of(warranty.plan.coveredParts.split(",")).stream()
            .map(String::trim).anyMatch(part -> part.equalsIgnoreCase(request.part().trim()));
        String note = String.join("; ", List.of(
            inTerm ? "Warranty is active" : "Warranty is outside its coverage dates",
            withinMileage ? "Vehicle is within the mileage limit" : "Vehicle mileage exceeds the plan limit",
            coveredPart ? "Part is covered by the plan" : "Part is not listed as covered"));
        WarrantyClaim claim = new WarrantyClaim();
        claim.warranty = warranty;
        claim.part = request.part().trim();
        claim.description = request.description().trim();
        claim.costEstimate = request.costEstimate();
        claim.validationNote = note;
        claim.status = inTerm && withinMileage && coveredPart ? ClaimStatus.SUBMITTED : ClaimStatus.REJECTED;
        return claimView(claims.save(claim));
    }

    @GetMapping("/finance/applications")
    @Transactional(readOnly = true)
    public List<FinanceView> financeApplications(Authentication auth) {
        List<FinanceApplication> items = hasRole(auth, UserRole.CREDIT_OFFICER, UserRole.ADMIN) ? applications.findAllByOrderByCreatedAtDesc()
            : applications.findByCustomerEmailOrderByCreatedAtDesc(auth.getName());
        return items.stream().map(this::financeView).toList();
    }

    @PostMapping("/finance/applications")
    @Transactional
    public FinanceView submitFinanceApplication(Authentication auth, @Valid @RequestBody FinanceRequest request) {
        Vehicle vehicle = ownedVehicle(request.vehicleId(), auth.getName());
        if (request.downPayment().compareTo(request.vehiclePrice()) >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Down payment must be lower than vehicle price");
        FinanceApplication application = new FinanceApplication();
        application.customer = user(auth.getName());
        application.vehicle = vehicle;
        application.type = request.type();
        application.vehiclePrice = request.vehiclePrice();
        application.downPayment = request.downPayment();
        application.requestedAmount = request.vehiclePrice().subtract(request.downPayment());
        application.tenureMonths = request.tenureMonths();
        application.monthlyIncome = request.monthlyIncome();
        application.existingEmi = request.existingEmi();
        application.bureauScore = request.bureauScore();
        application.incomeStable = request.incomeStable();

        double obligationsRatio = request.existingEmi().doubleValue() / request.monthlyIncome().doubleValue();
        int score = properties.getCredit().getBaseScore() + (int) Math.round(request.bureauScore() * 0.5)
            + (request.incomeStable() ? 80 : 0) + (obligationsRatio <= 0.30 ? 100 : obligationsRatio <= 0.45 ? 50 : 0);
        score = Math.max(300, Math.min(850, score));
        CreditDecision decision = score >= properties.getCredit().getApproveThreshold() ? CreditDecision.APPROVE
            : score >= properties.getCredit().getReviewThreshold() ? CreditDecision.REVIEW : CreditDecision.REJECT;
        RiskBand riskBand = score >= properties.getCredit().getApproveThreshold() ? RiskBand.A
            : score >= properties.getCredit().getReviewThreshold() ? RiskBand.B : RiskBand.C;
        double annualRate = riskBand == RiskBand.A ? properties.getRate().getBandA()
            : riskBand == RiskBand.B ? properties.getRate().getBandB() : properties.getRate().getBandC();
        String reasons = "Bureau contribution: " + request.bureauScore() + " x 0.5; stable income: "
            + (request.incomeStable() ? "+80" : "+0") + "; existing EMI ratio: "
            + String.format(Locale.ROOT, "%.0f%%", obligationsRatio * 100) + (obligationsRatio <= 0.30 ? " (+100)" : obligationsRatio <= 0.45 ? " (+50)" : " (+0)");
        application.status = decision == CreditDecision.APPROVE ? ApplicationStatus.APPROVED
            : decision == CreditDecision.REVIEW ? ApplicationStatus.MANUAL_REVIEW : ApplicationStatus.REJECTED;
        application.decisionNote = reasons;
        application = applications.save(application);

        CreditAssessment assessment = new CreditAssessment();
        assessment.application = application;
        assessment.score = score;
        assessment.riskBand = riskBand;
        assessment.decision = decision;
        assessment.annualRate = annualRate;
        assessment.reasons = reasons;
        assessments.save(assessment);
        if (decision == CreditDecision.APPROVE) financeContractService.createContract(application, annualRate);
        return financeView(application);
    }

    @GetMapping("/finance/installments")
    @Transactional(readOnly = true)
    public List<EmiView> installments(Authentication auth) {
        return schedules.findByContractApplicationCustomerEmailOrderByDueDateAsc(auth.getName()).stream().map(WorkflowController::emiView).toList();
    }

    @PostMapping("/finance/installments/{id}/pay")
    @Transactional
    public EmiView payInstallment(Authentication auth, @PathVariable Long id) {
        EmiSchedule emi = schedules.findById(id).filter(item -> item.contract.application.customer.email.equals(auth.getName()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installment not found"));
        if (emi.status == EmiStatus.PAID || payments.existsByEmiId(emi.id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Installment is already paid");
        if (emi.dueDate.isBefore(LocalDate.now()) && (emi.lateFee == null || emi.lateFee.signum() == 0)) {
            emi.lateFee = emi.emiAmount.multiply(BigDecimal.valueOf(properties.getLateFeePercent()).movePointLeft(2)).setScale(2, RoundingMode.HALF_UP);
            emi.status = EmiStatus.OVERDUE;
        }
        Payment payment = new Payment();
        payment.emi = emi;
        payment.amount = emi.emiAmount.add(emi.lateFee == null ? BigDecimal.ZERO : emi.lateFee);
        payment.paidAt = LocalDateTime.now();
        payments.save(payment);
        emi.status = EmiStatus.PAID;
        emi.paidAt = LocalDate.now();
        return emiView(emi);
    }

    private FinanceView financeView(FinanceApplication application) {
        CreditAssessment assessment = assessments.findByApplicationId(application.id).orElse(null);
        FinanceContract contract = contracts.findByApplicationId(application.id).orElse(null);
        return new FinanceView(application.id, application.type.name(), application.vehicle.make + " " + application.vehicle.model,
            application.requestedAmount, application.tenureMonths, application.status.name(), application.decisionNote,
            assessment == null ? null : assessment.score, assessment == null ? null : assessment.riskBand.name(),
            assessment == null ? null : assessment.annualRate, contract == null ? null : contract.monthlyEmi);
    }

    private UserAccount user(String email) { return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")); }
    private Vehicle ownedVehicle(Long id, String email) { return vehicles.findById(id).filter(item -> item.owner.email.equals(email)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found")); }
    private boolean hasRole(Authentication auth, UserRole... roles) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> List.of(roles).stream().anyMatch(role -> authority.equals("ROLE_" + role.name())));
    }
    private static VehicleView vehicleView(Vehicle vehicle) { return new VehicleView(vehicle.id, vehicle.vin, vehicle.make, vehicle.model, vehicle.year, vehicle.currentMileage); }
    private static WarrantyView warrantyView(VehicleWarranty item) {
        String status = item.status.name();
        if (item.status == WarrantyStatus.ACTIVE && LocalDate.now().isAfter(item.endDate)) status = WarrantyStatus.EXPIRED.name();
        return new WarrantyView(item.id, item.vehicle.id, item.vehicle.make + " " + item.vehicle.model,
            item.plan.name, item.startDate, item.endDate, status);
    }
    private static ClaimView claimView(WarrantyClaim item) { return new ClaimView(item.id, item.warranty.id, item.warranty.vehicle.make + " " + item.warranty.vehicle.model, item.part, item.description, item.costEstimate, item.status.name(), item.validationNote, item.reviewNote, item.createdAt); }
    private static EmiView emiView(EmiSchedule item) { return new EmiView(item.id, item.contract.application.id, item.installmentNo, item.dueDate, item.emiAmount, item.lateFee, item.status.name(), item.paidAt); }

}