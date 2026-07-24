package main.java.com.airtribe.meditrack.entity;

import java.util.Date;

// 1. The class is marked 'final' so it cannot be extended
public final class BillSummary {

    // 2. All fields are marked 'final' so they can only be set once
    private final String billId;
    private final String patientId;
    private final double totalAmount;
    private final Date billingDate;

    public BillSummary(String billId, String patientId, double totalAmount, Date billingDate) {
        this.billId = billId;
        this.patientId = patientId;
        this.totalAmount = totalAmount;

        // 3. Defensive Copying: We create a new Date object instead of storing the reference
        this.billingDate = new Date(billingDate.getTime());
    }

    // 4. ONLY Getters are provided. There are NO Setter methods.
    public String getBillId() { return billId; }
    public String getPatientId() { return patientId; }
    public double getTotalAmount() { return totalAmount; }

    // 5. Defensive Copying on the getter to prevent modification from the outside
    public Date getBillingDate() { return new Date(billingDate.getTime()); }
}