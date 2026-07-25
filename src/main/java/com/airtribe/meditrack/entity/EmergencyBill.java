package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

public class EmergencyBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;
    private final boolean afterHours;

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
