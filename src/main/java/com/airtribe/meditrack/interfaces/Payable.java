package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.constants.Constants;

public interface Payable {

    double getAmountDue();

    boolean processPayment(double amount);

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

    static String formatCurrency(double amount) {
        return Constants.CURRENCY_SYMBOL + String.format("%,.2f", amount);
    }
}
