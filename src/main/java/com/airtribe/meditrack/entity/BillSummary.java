package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.Payable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, thread-safe snapshot of a generated {@link Bill}.
 *
 * <p>This is the project's reference <b>immutable class</b>. Every rule is applied
 * deliberately:</p>
 *
 * <ol>
 *   <li><b>{@code final} class</b> — no subclass can add mutable state or override a
 *       getter to return something different on the second call.</li>
 *   <li><b>All fields {@code private final}</b> — assigned once, in the constructor.</li>
 *   <li><b>No setters</b> — and no method that mutates anything.</li>
 *   <li><b>Defensive copy in</b> — the incoming {@code List<LineItem>} is copied, so a
 *       caller holding the original list cannot reach in and change our state
 *       afterwards.</li>
 *   <li><b>Defensive copy / unmodifiable view out</b> — {@link #getLineItems()} returns
 *       a view that throws on mutation rather than the live list.</li>
 * </ol>
 *
 * <p><b>Why this is thread-safe.</b> The fields are {@code final} and are set before the
 * constructor returns, so the Java Memory Model guarantees any thread that sees a
 * reference to this object also sees fully-initialised fields — no synchronisation, no
 * volatile, no locks. There is no state to race on. This is why the reminder thread can
 * read summaries while the main thread bills, with no coordination at all.</p>
 *
 * <p>{@link Bill.LineItem} is itself immutable, so copying the list is genuinely enough —
 * had the elements been mutable, the list copy alone would have been a false comfort.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
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

    /**
     * @param billId          the bill's business key
     * @param patientId       the billed patient's id
     * @param patientName     the billed patient's name
     * @param billType        e.g. {@code "Consultation Bill"}
     * @param baseAmount      pre-policy charge
     * @param surchargeAmount any surcharge applied
     * @param taxAmount       tax component
     * @param totalAmount     final payable total
     * @param amountPaid      amount settled so far
     * @param billingDate     when the bill was raised
     * @param lineItems       the charged items; copied defensively
     */
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

    // ---------------------------------------------------------------- getters only
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

    /** @return an unmodifiable view; mutation attempts throw {@link UnsupportedOperationException} */
    public List<Bill.LineItem> getLineItems() {
        return lineItems;
    }

    // ------------------------------------------------------------------ derived
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

    /**
     * "Mutation" on an immutable type: returns a <em>new</em> summary with an extra
     * payment recorded, leaving this one untouched — the same approach
     * {@code String.trim()} and {@code LocalDate.plusDays()} take.
     *
     * @param additionalPayment the payment to record
     * @return a new summary reflecting the payment
     */
    public BillSummary withPayment(double additionalPayment) {
        return new BillSummary(billId, patientId, patientName, billType,
                baseAmount, surchargeAmount, taxAmount, totalAmount,
                Math.min(totalAmount, amountPaid + Math.max(0, additionalPayment)),
                billingDate, lineItems);
    }

    // ----------------------------------------------------------------- identity
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
