package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * An out-of-hours / emergency attendance bill.
 *
 * <p>The only bill type that overrides the {@link #applySurcharge(double)} hook, which
 * is what makes the Template Method visible: identical call
 * ({@code bill.generateBill()}), different result, because the run-time type supplies a
 * different step 3.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class EmergencyBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;
    private final boolean afterHours;

    /**
     * @param billId          business key
     * @param patient         who is billed
     * @param appointmentId   the appointment being settled
     * @param consultationFee the attending doctor's fee
     * @param afterHours      whether the attendance was outside clinic hours
     * @param strategy        pricing policy
     */
    public EmergencyBill(String billId, Patient patient, String appointmentId,
                         double consultationFee, boolean afterHours, BillingStrategy strategy) {
        super(billId, patient, appointmentId, strategy);
        this.consultationFee = consultationFee;
        this.afterHours = afterHours;

        addLineItem("Emergency attendance", consultationFee);
        addLineItem("Triage and observation", 350.0);
    }

    /** Step 1: attendance fee plus the standing triage charge. */
    @Override
    protected double calculateBaseAmount() {
        return sumLineItems();
    }

    /**
     * Step 3 — <b>hook overridden</b>. Emergency care attracts a surcharge, doubled when
     * the attendance falls outside clinic hours.
     *
     * @param amount the policy-adjusted amount
     * @return the surcharge to add
     */
    @Override
    protected double applySurcharge(double amount) {
        double rate = Constants.EMERGENCY_SURCHARGE_RATE * (afterHours ? 2 : 1);
        return amount * rate;
    }

    @Override
    public String getBillDescription() {
        return "Emergency Bill";
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public boolean isAfterHours() {
        return afterHours;
    }
}
