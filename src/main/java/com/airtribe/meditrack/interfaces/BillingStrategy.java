package com.airtribe.meditrack.interfaces;

@FunctionalInterface
public interface BillingStrategy {

    double calculate(double baseAmount);

    /**
     * @return a short label for this policy, shown on the printed bill
     */
    default String getStrategyName() {
        return getClass().getSimpleName();
    }

    /**
     * @return a one-line explanation of what this policy does
     */
    default String describe() {
        return getStrategyName() + " applied.";
    }
}
