package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

public class ConsultationBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;
    private final boolean firstVisit;

    public ConsultationBill(String billId, Patient patient, String appointmentId,
                            double consultationFee, boolean firstVisit, BillingStrategy strategy) {
        super(billId, patient, appointmentId, strategy);
        this.consultationFee = consultationFee;
        this.firstVisit = firstVisit;

        addLineItem("Doctor consultation", consultationFee);
        if (firstVisit) {
            addLineItem("New patient registration", Constants.REGISTRATION_FEE);
        }
    }

    /** Step 1 of the template: consultation fee plus registration if this is a first visit. */
    @Override
    protected double calculateBaseAmount() {
        return consultationFee + (firstVisit ? Constants.REGISTRATION_FEE : 0.0);
    }

    @Override
    public String getBillDescription() {
        return "Consultation Bill";
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public boolean isFirstVisit() {
        return firstVisit;
    }
}
