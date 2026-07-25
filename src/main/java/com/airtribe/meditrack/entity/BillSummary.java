package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.Payable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BillSummary implements Serializable, Comparable<BillSummary> {

    private static final long serialVersionUID = 1L;

    private final String billId;
    private final String patientId;
    private final String patientName;
    private final String billType;
    private final double baseAmount;
    private final double surchargeAmount;
    private final double taxAmount;
    private final double totalAmount;
    private final double amountPaid;
    private final LocalDateTime billingDate;

    /** Held as an unmodifiable list of immutable elements. */
    private final List<Bill.LineItem> lineItems;

    public BillSummary(String billId, String patientId, String patientName, String billType,
                       double baseAmount, double surchargeAmount, double taxAmount,
                       double totalAmount, double amountPaid,
                       LocalDateTime billingDate, List<Bill.LineItem> lineItems) {
        this.billId = billId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.billType = billType;
        this.baseAmount = baseAmount;
        this.surchargeAmount = surchargeAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        // LocalDateTime is immutable — unlike java.util.Date, it needs no defensive copy.
        this.billingDate = billingDate;
        // Defensive copy IN, then wrapped so it can never be mutated through our reference.
        this.lineItems = Collections.unmodifiableList(
                lineItems == null ? new ArrayList<>() : new ArrayList<>(lineItems));
    }

    public String getBillId() {
        return billId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBillType() {
        return billType;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public double getSurchargeAmount() {
        return surchargeAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public LocalDateTime getBillingDate() {
        return billingDate;
    }

    public List<Bill.LineItem> getLineItems() {
        return lineItems;
    }

    public double getBalanceDue() {
        return Math.max(0.0, totalAmount - amountPaid);
    }

    public boolean isFullyPaid() {
        return getBalanceDue() <= 0.0001;
    }

    public String getPaymentStatus() {
        if (isFullyPaid()) {
            return "PAID";
        }
        return amountPaid > 0 ? "PARTIALLY_PAID" : "UNPAID";
    }

    public BillSummary withPayment(double additionalPayment) {
        return new BillSummary(billId, patientId, patientName, billType,
                baseAmount, surchargeAmount, taxAmount, totalAmount,
                Math.min(totalAmount, amountPaid + Math.max(0, additionalPayment)),
                billingDate, lineItems);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillSummary other)) {
            return false;
        }
        return Objects.equals(billId, other.billId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billId);
    }

    /** Newest bill first. */
    @Override
    public int compareTo(BillSummary other) {
        return other.billingDate.compareTo(this.billingDate);
    }

    @Override
    public String toString() {
        return String.format("BillSummary{id=%s, patient=%s, type=%s, total=%s, status=%s}",
                billId, patientName, billType, Payable.formatCurrency(totalAmount), getPaymentStatus());
    }
}
