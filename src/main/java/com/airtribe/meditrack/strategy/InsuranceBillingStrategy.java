package com.airtribe.meditrack.strategy;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * Insurance-backed pricing: the provider covers a percentage, and the patient is billed
 * only the remaining co-pay.
 *
 * <p>The coverage rate is configurable per policy, so one class serves every insurer
 * rather than needing a subclass each.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class InsuranceBillingStrategy implements BillingStrategy {

    /** Fraction the insurer pays, {@code 0.0}–{@code 1.0}. */
    private final double coverageRate;

    private final String providerName;

    /** Uses the application default coverage rate. */
    public InsuranceBillingStrategy() {
        this(Constants.INSURANCE_COVERAGE_RATE, "Default Insurer");
    }

    /**
     * @param coverageRate fraction of the bill the insurer covers
     * @param providerName the insurer's name, shown on the bill
     */
    public InsuranceBillingStrategy(double coverageRate, String providerName) {
        this.coverageRate = Math.clamp(coverageRate, 0.0, 1.0);
        this.providerName = providerName == null ? "Insurer" : providerName;
    }

    /** @return the patient's co-pay after the insurer's share is removed */
    @Override
    public double calculate(double baseAmount) {
        return baseAmount * (1.0 - coverageRate);
    }

    public double getCoverageRate() {
        return coverageRate;
    }

    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getStrategyName() {
        return "Insurance (" + providerName + ")";
    }

    @Override
    public String describe() {
        return String.format("%s covers %.0f%%; patient pays the %.0f%% co-pay.",
                providerName, coverageRate * 100, (1 - coverageRate) * 100);
    }
}
