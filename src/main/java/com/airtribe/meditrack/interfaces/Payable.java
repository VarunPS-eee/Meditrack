package main.java.com.airtribe.meditrack.interfaces;


public interface Payable {
    /**
     * Retrieves the total amount due for payment.
     * @return the amount due
     */
    double getAmountDue();

    /**
     * Processes a payment for the given amount.
     * @param amount the amount being paid
     * @return true if the payment was successful, false otherwise
     */
    boolean processPayment(double amount);
}