package com.warrantywave.admin;

import com.warrantywave.dto.ClaimReviewRequest;
import com.warrantywave.dto.ClaimView;
import com.warrantywave.dto.FinanceDecisionRequest;
import com.warrantywave.dto.FinanceView;
import com.warrantywave.model.ApplicationStatus;
import com.warrantywave.model.ClaimStatus;
import com.warrantywave.model.CreditAssessment;
import com.warrantywave.model.FinanceApplication;
import com.warrantywave.model.FinanceContract;
import com.warrantywave.model.UserAccount;
import com.warrantywave.model.UserRole;
import com.warrantywave.model.WarrantyClaim;
import com.warrantywave.repository.CreditAssessmentRepository;
import com.warrantywave.repository.FinanceApplicationRepository;
import com.warrantywave.repository.FinanceContractRepository;
import com.warrantywave.repository.UserAccountRepository;
import com.warrantywave.repository.WarrantyClaimRepository;
import com.warrantywave.service.FinanceContractService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AdminReviewController {
    private final UserAccountRepository users;
    private final WarrantyClaimRepository claims;
    private final FinanceApplicationRepository applications;
    private final CreditAssessmentRepository assessments;
    private final FinanceContractRepository contracts;
    private final FinanceContractService financeContractService;

    public AdminReviewController(UserAccountRepository users, WarrantyClaimRepository claims,
                                 FinanceApplicationRepository applications, CreditAssessmentRepository assessments,
                                 FinanceContractRepository contracts, FinanceContractService financeContractService) {
        this.users = users;
        this.claims = claims;
        this.applications = applications;
        this.assessments = assessments;
        this.contracts = contracts;
        this.financeContractService = financeContractService;
    }

    @PatchMapping("/claims/{id}/review")
    @Transactional
    public ClaimView reviewClaim(Authentication auth, @PathVariable Long id, @Valid @RequestBody ClaimReviewRequest request) {
        requireRole(auth, UserRole.ADJUSTER, UserRole.ADMIN);
        WarrantyClaim claim = claims.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (claim.status == ClaimStatus.REJECTED || claim.status == ClaimStatus.SETTLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This claim is no longer reviewable");
        claim.status = request.approved() ? ClaimStatus.APPROVED : ClaimStatus.REJECTED;
        claim.reviewNote = request.note().trim();
        claim.reviewer = users.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        claim.reviewedAt = LocalDateTime.now();
        return claimView(claim);
    }

    @PatchMapping("/finance/applications/{id}/decision")
    @Transactional
    public FinanceView decideFinanceApplication(Authentication auth, @PathVariable Long id,
                                                 @Valid @RequestBody FinanceDecisionRequest request) {
        requireRole(auth, UserRole.CREDIT_OFFICER, UserRole.ADMIN);
        FinanceApplication application = applications.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (application.status != ApplicationStatus.MANUAL_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not awaiting manual review");
        CreditAssessment assessment = assessments.findByApplicationId(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Credit assessment is missing"));
        application.status = request.approved() ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED;
        application.decisionNote = request.note().trim();
        if (request.approved()) financeContractService.createContract(application, assessment.annualRate);
        return financeView(application);
    }

    private void requireRole(Authentication auth, UserRole... roles) {
        boolean authorized = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> List.of(roles).stream().anyMatch(role -> authority.equals("ROLE_" + role.name())));
        if (!authorized) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
    }

    private FinanceView financeView(FinanceApplication application) {
        CreditAssessment assessment = assessments.findByApplicationId(application.id).orElse(null);
        FinanceContract contract = contracts.findByApplicationId(application.id).orElse(null);
        return new FinanceView(application.id, application.type.name(),
            application.vehicle.make + " " + application.vehicle.model, application.requestedAmount,
            application.tenureMonths, application.status.name(), application.decisionNote,
            assessment == null ? null : assessment.score,
            assessment == null ? null : assessment.riskBand.name(),
            assessment == null ? null : assessment.annualRate, contract == null ? null : contract.monthlyEmi);
    }

    private static ClaimView claimView(WarrantyClaim item) {
        return new ClaimView(item.id, item.warranty.id, item.warranty.vehicle.make + " " + item.warranty.vehicle.model,
            item.part, item.description, item.costEstimate, item.status.name(), item.validationNote, item.reviewNote,
            item.createdAt);
    }

}