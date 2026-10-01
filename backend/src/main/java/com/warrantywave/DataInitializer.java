package com.warrantywave;

import com.warrantywave.model.UserAccount;
import com.warrantywave.model.UserRole;
import com.warrantywave.model.WarrantyPlan;
import com.warrantywave.repository.UserAccountRepository;
import com.warrantywave.repository.WarrantyPlanRepository;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    ApplicationRunner initializeData(WarrantyPlanRepository plans, UserAccountRepository users,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${warrantywave.bootstrap.admin-email:}") String adminEmail,
                                     @Value("${warrantywave.bootstrap.admin-password:}") String adminPassword) {
        return args -> {
            if (plans.count() == 0) {
                plans.save(plan("Essential", "Engine,Transmission,Electrical", 24, 40000, "899.00"));
                plans.save(plan("Complete", "Engine,Transmission,Electrical,Air Conditioning,Steering,Suspension", 48, 80000, "1699.00"));
                plans.save(plan("Extended", "Engine,Transmission,Electrical,Air Conditioning,Steering,Suspension,Brakes", 60, 100000, "2299.00"));
            }
            if (!adminEmail.isBlank() && !adminPassword.isBlank()) {
                String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
                if (!users.existsByEmail(normalizedEmail)) {
                    users.save(new UserAccount("WarrantyWave Admin", normalizedEmail,
                        passwordEncoder.encode(adminPassword), UserRole.ADMIN));
                }
            }
        };
    }

    private WarrantyPlan plan(String name, String coveredParts, int durationMonths, int kmLimit, String price) {
        WarrantyPlan plan = new WarrantyPlan();
        plan.name = name;
        plan.coveredParts = coveredParts;
        plan.durationMonths = durationMonths;
        plan.kmLimit = kmLimit;
        plan.price = new BigDecimal(price);
        plan.active = true;
        return plan;
    }
}