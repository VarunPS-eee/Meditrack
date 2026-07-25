package com.airtribe.meditrack.strategy;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * Concessional pricing for patients aged {@value com.airtribe.meditrack.constants.Constants#SENIOR_CITIZEN_AGE}
 * and above.
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class SeniorCitizenBillingStrategy implements BillingStrategy {

    private final double discountRate;

    public SeniorCitizenBillingStrategy() {
        this(Constants.SENIOR_CITIZEN_DISCOUNT_RATE);
    }

    /**
     * @param discountRate fraction to deduct, {@code 0.0}–{@code 1.0}
     */
    public SeniorCitizenBillingStrategy(double discountRate) {
        this.discountRate = Math.clamp(discountRate, 0.0, 1.0);
    }

    @Override
    public double calculate(double baseAmount) {
        return baseAmount * (1.0 - discountRate);
    }

    public double getDiscountRate() {
        return discountRate;
    }

    @Override
    public String getStrategyName() {
        return "Senior Citizen";
    }

    @Override
    public String describe() {
        return String.format("Senior citizen concession of %.0f%% applied.", discountRate * 100);
    }
}
