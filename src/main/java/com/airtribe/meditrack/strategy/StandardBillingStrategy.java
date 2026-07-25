package com.airtribe.meditrack.strategy;

import com.airtribe.meditrack.interfaces.BillingStrategy;

public class StandardBillingStrategy implements BillingStrategy {

    @Override
    public double calculate(double baseAmount) {
        return baseAmount;
    }

    @Override
    public String getStrategyName() {
        return "Standard";
    }

    @Override
    public String describe() {
        return "Standard rate — no adjustment applied.";
    }
}
