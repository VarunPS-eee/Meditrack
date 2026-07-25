package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.BillingStrategy;

public class ProcedureBill extends Bill {

    private static final long serialVersionUID = 1L;

    private final double consultationFee;

    public ProcedureBill(String billId, Patient patient, String appointmentId,
                         double consultationFee, BillingStrategy strategy) {
        super(billId, patient, appointmentId, strategy);
        this.consultationFee = consultationFee;
        addLineItem("Doctor consultation", consultationFee);
    }

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
