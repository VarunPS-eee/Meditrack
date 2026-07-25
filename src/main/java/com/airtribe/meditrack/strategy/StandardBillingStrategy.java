package com.airtribe.meditrack.strategy;

import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * The default pricing policy: charge exactly what was quoted.
 *
 * <p>Having an explicit "do nothing" strategy rather than a {@code null} check means the
 * calling code never branches on whether a policy exists — the Null Object pattern
 * applied to Strategy.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
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
