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

public abstract class Bill extends MedicalEntity implements Payable {

    private static final long serialVersionUID = 1L;

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

    private final Patient patient;
    private final String appointmentId;
    private final LocalDateTime billDate;
    private final List<LineItem> lineItems;

    private transient BillingStrategy billingStrategy;

    private double amountPaid;

    // Computed by generateBill(); retained so the printed bill can show the breakdown.
    private double baseAmount;
    private double afterStrategyAmount;
    private double surchargeAmount;
    private double taxAmount;
    private double totalAmount;
    private boolean generated;

    protected Bill(String billId, Patient patient, String appointmentId, BillingStrategy strategy) {
        super(billId);
        this.patient = patient;
        this.appointmentId = appointmentId;
        this.billDate = LocalDateTime.now();
        this.lineItems = new ArrayList<>();
        this.billingStrategy = strategy == null ? amount -> amount : strategy;
        this.amountPaid = 0.0;
    }

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

    protected abstract double calculateBaseAmount();

    protected double applyStrategy(double base) {
        return billingStrategy == null ? base : billingStrategy.calculate(base);
    }

    protected double applySurcharge(double amount) {
        return 0.0;
    }

    protected double calculateTax(double taxableAmount) {
        return taxableAmount * Constants.TAX_RATE;
    }

    /**
     * @return a short label describing this bill type, overridden by each subclass
     */
    public abstract String getBillDescription();

    @Override
    public double getAmountDue() {
        ensureGenerated();
        return Math.max(0.0, totalAmount - amountPaid);
    }

    @Override
    public double getAmountPaid() {
        return amountPaid;
    }

    @Override
    public boolean processPayment(double amount) {
        if (amount <= 0 || amount > getAmountDue() + 0.0001) {
            return false;
        }
        this.amountPaid += amount;
        touch();
        return true;
    }

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
