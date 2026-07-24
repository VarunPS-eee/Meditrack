package main.java.com.airtribe.meditrack.entity;

import java.util.Date;

public class Bill {
    private String billId;
    private Patient patient;
    private double totalAmount;
    private Date billDate;

    public Bill(String billId, Patient patient, double totalAmount, Date billDate) {
        this.billId = billId;
        this.patient = patient;
        this.totalAmount = totalAmount;
        this.billDate = billDate;
    }

    // Getters and Setters
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Date getBillDate() { return billDate; }
    public void setBillDate(Date billDate) { this.billDate = billDate; }
}