package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * The ordinary outpatient bill: one consultation fee, plus a one-off registration fee
 * for a patient's first visit.
 *
 * <p>Overrides only {@link #calculateBaseAmount()} — it accepts the inherited surcharge
 * hook (none) and tax hook (flat GST) unchanged. That is the Template Method working as
 * intended: a subclass writes the one step it actually differs on.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class ConsultationBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;
    private final boolean firstVisit;

    /**
     * @param billId          business key
     * @param patient         who is billed
     * @param appointmentId   the appointment being settled
     * @param consultationFee the doctor's fee
     * @param firstVisit      whether to add the registration fee
     * @param strategy        pricing policy
     */
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
