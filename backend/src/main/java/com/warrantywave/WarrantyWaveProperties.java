package com.warrantywave;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "warrantywave")
public class WarrantyWaveProperties {
    private String frontendOrigin = "http://localhost:4200";
    private final Jwt jwt = new Jwt();
    private final Credit credit = new Credit();
    private final Rate rate = new Rate();
    private double lateFeePercent = 2.0;

    public String getFrontendOrigin() { return frontendOrigin; }
    public void setFrontendOrigin(String frontendOrigin) { this.frontendOrigin = frontendOrigin; }
    public Jwt getJwt() { return jwt; }
    public Credit getCredit() { return credit; }
    public Rate getRate() { return rate; }
    public double getLateFeePercent() { return lateFeePercent; }
    public void setLateFeePercent(double lateFeePercent) { this.lateFeePercent = lateFeePercent; }

    public static class Jwt {
        private String secret;
        private long expiryMinutes = 60;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(long expiryMinutes) { this.expiryMinutes = expiryMinutes; }
    }

    public static class Credit {
        private int baseScore = 300;
        private int approveThreshold = 700;
        private int reviewThreshold = 550;
        public int getBaseScore() { return baseScore; }
        public void setBaseScore(int baseScore) { this.baseScore = baseScore; }
        public int getApproveThreshold() { return approveThreshold; }
        public void setApproveThreshold(int approveThreshold) { this.approveThreshold = approveThreshold; }
        public int getReviewThreshold() { return reviewThreshold; }
        public void setReviewThreshold(int reviewThreshold) { this.reviewThreshold = reviewThreshold; }
    }

    public static class Rate {
        private double bandA = 8.5;
        private double bandB = 11.0;
        private double bandC = 14.0;
        public double getBandA() { return bandA; }
        public void setBandA(double bandA) { this.bandA = bandA; }
        public double getBandB() { return bandB; }
        public void setBandB(double bandB) { this.bandB = bandB; }
        public double getBandC() { return bandC; }
        public void setBandC(double bandC) { this.bandC = bandC; }
    }
}
