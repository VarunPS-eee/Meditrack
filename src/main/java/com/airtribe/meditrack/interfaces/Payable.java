package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.constants.Constants;

/**
 * Contract for anything that can be charged for and paid.
 *
 * <p>Demonstrates a mix of abstract methods, {@code default} methods that build on
 * them, and a {@code static} helper — the three method kinds a modern Java interface
 * can carry.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public interface Payable {

    /**
     * The amount still owed, after taxes, discounts and any payments already made.
     *
     * @return the outstanding amount, never negative
     */
    double getAmountDue();

    /**
     * Records a payment against this payable.
     *
     * @param amount the amount being paid
     * @return {@code true} if the payment was accepted, {@code false} otherwise
     */
    boolean processPayment(double amount);

    /**
     * Total already paid against this payable.
     *
     * @return the cumulative amount paid
     */
    double getAmountPaid();

    /**
     * @return {@code true} once nothing further is owed
     */
    default boolean isFullyPaid() {
        return getAmountDue() <= 0.0001;
    }

    /**
     * @return {@code true} when some — but not all — of the balance has been settled
     */
    default boolean isPartiallyPaid() {
        return getAmountPaid() > 0 && !isFullyPaid();
    }

    /**
     * @return a human-readable payment status for console output
     */
    default String getPaymentStatus() {
        if (isFullyPaid()) {
            return "PAID";
        }
        return isPartiallyPaid() ? "PARTIALLY_PAID" : "UNPAID";
    }

    /**
     * Formats an amount with the application currency symbol and two decimals.
     *
     * <p>A {@code static} interface method: shared helper logic that belongs to the
     * contract itself rather than to any one implementation.</p>
     *
     * @param amount the amount to format
     * @return e.g. {@code ₹1,250.00}
     */
    static String formatCurrency(double amount) {
        return Constants.CURRENCY_SYMBOL + String.format("%,.2f", amount);
    }
}
