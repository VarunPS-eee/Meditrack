package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.BillingStrategy;

/**
 * A bill covering a consultation plus one or more clinical procedures.
 *
 * <p>Its base amount is driven by the accumulated line items rather than a single fee,
 * showing that a Template Method step can be computed from collaborating state instead
 * of a constructor argument.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class ProcedureBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;

    /**
     * @param billId          business key
     * @param patient         who is billed
     * @param appointmentId   the appointment being settled
     * @param consultationFee the doctor's fee
     * @param strategy        pricing policy
     */
    public ProcedureBill(String billId, Patient patient, String appointmentId,
                         double consultationFee, BillingStrategy strategy) {
        super(billId, patient, appointmentId, strategy);
        this.consultationFee = consultationFee;
        addLineItem("Doctor consultation", consultationFee);
    }

    /**
     * Adds a procedure to this bill. Totals are recomputed on the next
     * {@code generateBill()} / getter call.
     *
     * @param name     the procedure performed
     * @param cost     unit cost
     * @param quantity how many times it was performed
     */
    public void addProcedure(String name, double cost, int quantity) {
        addLineItem("Procedure: " + name, cost, quantity);
    }

    public void addProcedure(String name, double cost) {
        addProcedure(name, cost, 1);
    }

    /** Step 1 of the template: everything on the itemised list. */
    @Override
    protected double calculateBaseAmount() {
        return sumLineItems();
    }

    @Override
    public String getBillDescription() {
        return "Procedure Bill";
    }

    public double getConsultationFee() {
        return consultationFee;
    }
}
