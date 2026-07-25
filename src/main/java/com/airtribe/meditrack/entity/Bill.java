package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.BillingStrategy;
import com.airtribe.meditrack.interfaces.Payable;
import com.airtribe.meditrack.util.DateUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base for every kind of bill — the project's <b>Template Method</b> pattern.
 *
 * <p>{@link #generateBill()} is {@code final}: the <em>sequence</em> of billing steps is
 * a business invariant and no subclass may reorder it, skip the tax step, or forget to
 * record a total. What each subclass may change is the content of individual steps:</p>
 *
 * <table border="1">
 *   <caption>Template steps</caption>
 *   <tr><th>Step</th><th>Kind</th><th>Who decides</th></tr>
 *   <tr><td>{@link #calculateBaseAmount()}</td><td>abstract</td><td>subclass must implement</td></tr>
 *   <tr><td>{@link #applyStrategy(double)}</td><td>concrete</td><td>injected {@link BillingStrategy}</td></tr>
 *   <tr><td>{@link #applySurcharge(double)}</td><td>hook</td><td>subclass may override; default no-op</td></tr>
 *   <tr><td>{@link #calculateTax(double)}</td><td>hook</td><td>subclass may override; default GST</td></tr>
 * </table>
 *
 * <p>The net effect is polymorphic billing: three {@code Bill} references, one call to
 * {@code generateBill()}, three different totals — resolved at run time by dynamic
 * dispatch into the overridden steps.</p>
 *
 * <p>Also implements {@link Payable}, so a bill can be paid down incrementally.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public abstract class Bill extends MedicalEntity implements Payable {

    private static final long serialVersionUID = 1L;

    /**
     * One charged item on a bill. A nested, immutable value type — it has no identity
     * of its own and exists only as part of its enclosing {@code Bill}.
     */
    public static final class LineItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String description;
        private final double amount;
        private final int quantity;

        public LineItem(String description, double amount, int quantity) {
            this.description = description;
            this.amount = amount;
            this.quantity = Math.max(1, quantity);
        }

        public LineItem(String description, double amount) {
            this(description, amount, 1);
        }

        public String getDescription() {
            return description;
        }

        public double getAmount() {
            return amount;
        }

        public int getQuantity() {
            return quantity;
        }

        /** @return {@code amount × quantity} */
        public double getLineTotal() {
            return amount * quantity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LineItem other)) {
                return false;
            }
            return Double.compare(amount, other.amount) == 0
                    && quantity == other.quantity
                    && Objects.equals(description, other.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, amount, quantity);
        }

        @Override
        public String toString() {
            return description + " x" + quantity + " = " + Payable.formatCurrency(getLineTotal());
        }
    }

    // ------------------------------------------------------------------- state
    private final Patient patient;
    private final String appointmentId;
    private final LocalDateTime billDate;
    private final List<LineItem> lineItems;

    /**
     * Injected pricing policy — the Strategy.
     *
     * <p>Marked {@code transient} because a strategy is behaviour, not data: it is often
     * supplied as a lambda, which is not {@link Serializable}. On reload the bill keeps
     * its already-computed totals and {@link #applyStrategy(double)} falls back to
     * identity, so a deserialized bill never silently reprices itself.</p>
     */
    private transient BillingStrategy billingStrategy;

    private double amountPaid;

    // Computed by generateBill(); retained so the printed bill can show the breakdown.
    private double baseAmount;
    private double afterStrategyAmount;
    private double surchargeAmount;
    private double taxAmount;
    private double totalAmount;
    private boolean generated;

    /**
     * @param billId        business key
     * @param patient       who is being charged
     * @param appointmentId the appointment this bill settles, may be {@code null}
     * @param strategy      the pricing policy to apply
     */
    protected Bill(String billId, Patient patient, String appointmentId, BillingStrategy strategy) {
        super(billId);
        this.patient = patient;
        this.appointmentId = appointmentId;
        this.billDate = LocalDateTime.now();
        this.lineItems = new ArrayList<>();
        this.billingStrategy = strategy == null ? amount -> amount : strategy;
        this.amountPaid = 0.0;
    }

    // --------------------------------------------------------- template method
    /**
     * <b>The template method.</b> Runs the billing algorithm in a fixed order and
     * returns an immutable snapshot of the result.
     *
     * <p>{@code final} on purpose — subclasses customise the steps, never the sequence.</p>
     *
     * @return an immutable {@link BillSummary} of the computed bill
     */
    public final BillSummary generateBill() {
        // Step 1 — what is being charged for (subclass decides).
        this.baseAmount = calculateBaseAmount();

        // Step 2 — apply the injected pricing policy (Strategy decides).
        this.afterStrategyAmount = applyStrategy(this.baseAmount);

        // Step 3 — bill-type-specific surcharge (hook; default adds nothing).
        this.surchargeAmount = applySurcharge(this.afterStrategyAmount);

        double taxable = this.afterStrategyAmount + this.surchargeAmount;

        // Step 4 — tax (hook; default is flat GST).
        this.taxAmount = calculateTax(taxable);

        // Step 5 — invariant total, computed the same way for every bill type.
        this.totalAmount = Math.max(0.0, taxable + this.taxAmount);
        this.generated = true;
        touch();

        return toSummary();
    }

    /**
     * Step 1 — the pre-policy charge. Every bill type must answer this.
     *
     * @return the base amount before policy, surcharge and tax
     */
    protected abstract double calculateBaseAmount();

    /**
     * Step 2 — delegates to the injected {@link BillingStrategy}.
     *
     * @param base the amount from step 1
     * @return the policy-adjusted amount
     */
    protected double applyStrategy(double base) {
        return billingStrategy == null ? base : billingStrategy.calculate(base);
    }

    /**
     * Step 3 — <b>hook</b>. Default implementation adds no surcharge; subclasses that
     * need one (e.g. {@link EmergencyBill}) override it.
     *
     * @param amount the policy-adjusted amount
     * @return the surcharge to add, {@code 0.0} by default
     */
    protected double applySurcharge(double amount) {
        return 0.0;
    }

    /**
     * Step 4 — <b>hook</b>. Default is the flat GST rate from
     * {@link Constants#TAX_RATE}; a subclass may override to apply a different regime.
     *
     * @param taxableAmount the amount tax is charged on
     * @return the tax component
     */
    protected double calculateTax(double taxableAmount) {
        return taxableAmount * Constants.TAX_RATE;
    }

    /**
     * @return a short label describing this bill type, overridden by each subclass
     */
    public abstract String getBillDescription();

    // ------------------------------------------------------------------ payable
    @Override
    public double getAmountDue() {
        ensureGenerated();
        return Math.max(0.0, totalAmount - amountPaid);
    }

    @Override
    public double getAmountPaid() {
        return amountPaid;
    }

    /**
     * Records a payment. Rejects non-positive amounts and anything above the
     * outstanding balance, so a bill can never go into credit.
     *
     * @param amount the amount tendered
     * @return {@code true} if accepted
     */
    @Override
    public boolean processPayment(double amount) {
        if (amount <= 0 || amount > getAmountDue() + 0.0001) {
            return false;
        }
        this.amountPaid += amount;
        touch();
        return true;
    }

    // ------------------------------------------------------------- line items
    public void addLineItem(LineItem item) {
        if (item != null) {
            lineItems.add(item);
            this.generated = false; // totals are stale until regenerated
            touch();
        }
    }

    public void addLineItem(String description, double amount) {
        addLineItem(new LineItem(description, amount));
    }

    public void addLineItem(String description, double amount, int quantity) {
        addLineItem(new LineItem(description, amount, quantity));
    }

    public List<LineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    /** @return the summed total of all line items */
    protected double sumLineItems() {
        return lineItems.stream().mapToDouble(LineItem::getLineTotal).sum();
    }

    // -------------------------------------------------------------- accessors
    public Patient getPatient() {
        return patient;
    }

    public String getBillId() {
        return getId();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public BillingStrategy getBillingStrategy() {
        return billingStrategy;
    }

    public void setBillingStrategy(BillingStrategy billingStrategy) {
        this.billingStrategy = billingStrategy == null ? amount -> amount : billingStrategy;
        this.generated = false;
        touch();
    }

    public double getBaseAmount() {
        ensureGenerated();
        return baseAmount;
    }

    public double getSurchargeAmount() {
        ensureGenerated();
        return surchargeAmount;
    }

    public double getTaxAmount() {
        ensureGenerated();
        return taxAmount;
    }

    public double getTotalAmount() {
        ensureGenerated();
        return totalAmount;
    }

    public boolean isGenerated() {
        return generated;
    }

    /** Lazily runs the template if a caller asks for totals before generating. */
    private void ensureGenerated() {
        if (!generated) {
            generateBill();
        }
    }

    // ---------------------------------------------------------------- output
    /**
     * @return an immutable snapshot of this bill, safe to hand to any caller
     */
    public BillSummary toSummary() {
        ensureGenerated();
        return new BillSummary(
                getId(),
                patient == null ? "UNKNOWN" : patient.getId(),
                patient == null ? "Unknown" : patient.getName(),
                getBillDescription(),
                baseAmount,
                surchargeAmount,
                taxAmount,
                totalAmount,
                amountPaid,
                billDate,
                lineItems);
    }

    /**
     * Renders the printable bill.
     *
     * <p>Built with a single {@link StringBuilder} rather than repeated {@code +}
     * concatenation: this method appends ~20 fragments in a loop, and {@code +} inside
     * a loop allocates a fresh {@code String} on every iteration. One mutable buffer,
     * one final {@code toString()}.</p>
     *
     * @return the full bill as printable text
     */
    public String getFormattedBill() {
        StringBuilder sb = new StringBuilder(512);
        String line = "=".repeat(62);

        sb.append(line).append('\n')
                .append(centre(Constants.APP_NAME + " — " + getBillDescription().toUpperCase())).append('\n')
                .append(line).append('\n')
                .append(String.format("  Bill ID    : %s%n", getId()))
                .append(String.format("  Patient    : %s (%s)%n",
                        patient == null ? "Unknown" : patient.getName(),
                        patient == null ? "-" : patient.getId()))
                .append(String.format("  Appointment: %s%n", appointmentId == null ? "-" : appointmentId))
                .append(String.format("  Date       : %s%n", DateUtil.formatForDisplay(billDate)))
                .append(String.format("  Policy     : %s%n", getStrategyLabel()))
                .append("-".repeat(62)).append('\n');

        if (!lineItems.isEmpty()) {
            sb.append("  ITEMS\n");
            for (LineItem item : lineItems) {
                sb.append(String.format("    %-34s x%-3d %14s%n",
                        truncate(item.getDescription(), 34),
                        item.getQuantity(),
                        Payable.formatCurrency(item.getLineTotal())));
            }
            sb.append("-".repeat(62)).append('\n');
        }

        sb.append(String.format("  %-42s %16s%n", "Base amount", Payable.formatCurrency(baseAmount)));
        if (Math.abs(afterStrategyAmount - baseAmount) > 0.0001) {
            sb.append(String.format("  %-42s %16s%n",
                    "Policy adjustment (" + getStrategyLabel() + ")",
                    Payable.formatCurrency(afterStrategyAmount - baseAmount)));
        }
        if (surchargeAmount > 0) {
            sb.append(String.format("  %-42s %16s%n", "Surcharge", Payable.formatCurrency(surchargeAmount)));
        }
        sb.append(String.format("  %-42s %16s%n",
                        String.format("GST @ %.0f%%", Constants.TAX_RATE * 100), Payable.formatCurrency(taxAmount)))
                .append("-".repeat(62)).append('\n')
                .append(String.format("  %-42s %16s%n", "TOTAL", Payable.formatCurrency(totalAmount)))
                .append(String.format("  %-42s %16s%n", "Paid", Payable.formatCurrency(amountPaid)))
                .append(String.format("  %-42s %16s%n", "Balance due", Payable.formatCurrency(getAmountDue())))
                .append(String.format("  %-42s %16s%n", "Status", getPaymentStatus()))
                .append(line);

        return sb.toString();
    }

    /** @return the strategy's label, or {@code "Standard"} after deserialization */
    private String getStrategyLabel() {
        return billingStrategy == null ? "Standard" : billingStrategy.getStrategyName();
    }

    private static String centre(String text) {
        int pad = Math.max(0, (62 - text.length()) / 2);
        return " ".repeat(pad) + text;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    // -------------------------------------------------------------- behaviour
    @Override
    public String getEntityType() {
        return "Bill";
    }

    @Override
    public String getSearchableText() {
        return String.join(" ",
                getId(),
                patient == null ? "" : patient.getName(),
                patient == null ? "" : patient.getId(),
                getBillDescription(),
                getPaymentStatus());
    }

    @Override
    public void displayDetails() {
        ensureGenerated();
        System.out.printf("  [%s] %-14s | Patient: %-18s | Total: %12s | %s%n",
                getId(),
                getBillDescription(),
                patient == null ? "Unknown" : patient.getName(),
                Payable.formatCurrency(totalAmount),
                getPaymentStatus());
    }

    @Override
    public String toString() {
        return getBillDescription() + "{id='" + getId() + "', total=" + totalAmount + '}';
    }
}
