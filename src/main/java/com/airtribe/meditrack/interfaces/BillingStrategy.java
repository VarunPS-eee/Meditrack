package com.airtribe.meditrack.interfaces;

/**
 * Strategy Pattern — the algorithm interface for turning a base charge into a payable total.
 *
 * <p>Each implementation encapsulates one pricing policy (standard, insurance-backed,
 * senior-citizen concession…). The billing service holds a {@code BillingStrategy}
 * reference and delegates, so adding a new policy never edits existing code —
 * the Open/Closed Principle in practice.</p>
 *
 * <p>Annotated {@link FunctionalInterface} so a policy can also be supplied inline
 * as a lambda where a full class would be overkill.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
@FunctionalInterface
public interface BillingStrategy {

    /**
     * Applies this pricing policy to a base amount.
     *
     * @param baseAmount the pre-policy charge (consultation fee, procedure cost, …)
     * @return the amount after this policy's adjustments, before tax
     */
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
