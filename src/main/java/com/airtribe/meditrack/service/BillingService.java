package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillSummary;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.EntityNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.factory.BillFactory;
import com.airtribe.meditrack.interfaces.BillingStrategy;
import com.airtribe.meditrack.util.DataStore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Raises and settles bills.
 *
 * <p>The <b>context</b> in the Strategy pattern and the sole caller of
 * {@link BillFactory}. Notice how little this class does: it delegates <em>which</em>
 * bill to the factory, <em>how much</em> to the strategy, and the calculation order to
 * the Template Method inside {@link Bill}. What remains here is the use-case
 * orchestration — which is exactly the Single Responsibility Principle's promise.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI) / Varun (Factory)
 */
public class BillingService {

    private final DataStore<Bill> store;

    public BillingService() {
        this(new DataStore<>("Bill"));
    }

    public BillingService(DataStore<Bill> store) {
        this.store = store;
    }

    // -------------------------------------------------------------------- create
    /**
     * Raises a bill for a completed appointment.
     *
     * @param appointment the appointment to bill
     * @param type        which kind of bill to raise
     * @return the generated bill
     * @throws InvalidDataException if the appointment is missing or not yet completed
     */
    public Bill generateBillForAppointment(Appointment appointment, BillType type)
            throws InvalidDataException {

        if (appointment == null) {
            throw new InvalidDataException("appointment", null, "is required to raise a bill");
        }
        if (!appointment.getStatus().isBillable()) {
            throw new InvalidDataException("status", appointment.getStatus(),
                    "only a COMPLETED appointment can be billed");
        }

        Bill bill = BillFactory.createBillForAppointment(type, appointment);
        bill.generateBill();          // run the template
        return store.save(bill);
    }

    /**
     * Raises a standalone bill not tied to an appointment.
     *
     * @param patient the patient to bill
     * @param type    which kind of bill
     * @param baseFee the base charge
     * @return the generated bill
     * @throws InvalidDataException if the patient is missing
     */
    public Bill generateBill(Patient patient, BillType type, double baseFee)
            throws InvalidDataException {
        if (patient == null) {
            throw new InvalidDataException("patient", null, "is required to raise a bill");
        }
        Bill bill = BillFactory.createBill(type, patient, null, baseFee);
        bill.generateBill();
        return store.save(bill);
    }

    /**
     * Raises a bill with an explicitly chosen pricing policy, overriding the factory's
     * automatic choice.
     *
     * @param patient  the patient to bill
     * @param type     which kind of bill
     * @param baseFee  the base charge
     * @param strategy the policy to apply
     * @return the generated bill
     * @throws InvalidDataException if the patient is missing
     */
    public Bill generateBillWithStrategy(Patient patient, BillType type,
                                         double baseFee, BillingStrategy strategy)
            throws InvalidDataException {
        if (patient == null) {
            throw new InvalidDataException("patient", null, "is required to raise a bill");
        }
        Bill bill = BillFactory.createBill(type, patient, null, baseFee, strategy);
        bill.generateBill();
        return store.save(bill);
    }

    // ------------------------------------------------------------------ payment
    /**
     * Records a payment against a bill.
     *
     * @param billId the bill to pay
     * @param amount the amount tendered
     * @return the updated bill
     * @throws EntityNotFoundException if the bill id is unknown
     * @throws InvalidDataException    if the amount is invalid or exceeds the balance
     */
    public Bill recordPayment(String billId, double amount)
            throws EntityNotFoundException, InvalidDataException {
        Bill bill = store.getById(billId);
        if (!bill.processPayment(amount)) {
            throw new InvalidDataException("amount", amount,
                    String.format("must be positive and at most the balance due (%.2f)",
                            bill.getAmountDue()));
        }
        return bill;
    }

    /**
     * Settles a bill in full.
     *
     * @param billId the bill to settle
     * @return the paid bill
     * @throws EntityNotFoundException if the bill id is unknown
     * @throws InvalidDataException    if the bill is already fully paid
     */
    public Bill settleInFull(String billId) throws EntityNotFoundException, InvalidDataException {
        Bill bill = store.getById(billId);
        return recordPayment(billId, bill.getAmountDue());
    }

    // ---------------------------------------------------------------------- read
    public Optional<Bill> findById(String billId) {
        return store.findById(billId);
    }

    public List<Bill> getAllBills() {
        return store.findAll();
    }

    public int count() {
        return store.count();
    }

    /**
     * @param patientId the patient
     * @return that patient's bills
     */
    public List<Bill> getBillsForPatient(String patientId) {
        return store.findBy(b -> b.getPatient() != null && b.getPatient().getId().equals(patientId));
    }

    /** @return bills with an outstanding balance, largest first */
    public List<Bill> getUnpaidBills() {
        return store.stream()
                .filter(b -> !b.isFullyPaid())
                .sorted(Comparator.comparingDouble(Bill::getAmountDue).reversed())
                .collect(Collectors.toList());
    }

    /**
     * @param keyword the search term
     * @return matching bills
     */
    public List<Bill> searchBills(String keyword) {
        return store.search(keyword);
    }

    /** @return an immutable summary of every bill, newest first */
    public List<BillSummary> getAllSummaries() {
        return store.stream()
                .map(Bill::toSummary)
                .sorted()
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------- streams & analytics
    /**
     * @return the sum of every bill's total
     */
    public double getTotalBilled() {
        return store.stream().mapToDouble(Bill::getTotalAmount).sum();
    }

    /**
     * @return the sum of every payment received
     */
    public double getTotalCollected() {
        return store.stream().mapToDouble(Bill::getAmountPaid).sum();
    }

    /**
     * @return the total still outstanding across all bills
     */
    public double getTotalOutstanding() {
        return store.stream().mapToDouble(Bill::getAmountDue).sum();
    }

    /**
     * @return total tax charged, for the clinic's GST return
     */
    public double getTotalTaxCollected() {
        return store.stream().mapToDouble(Bill::getTaxAmount).sum();
    }

    /**
     * @return revenue grouped by bill type
     */
    public Map<String, Double> getRevenueByBillType() {
        return store.stream()
                .collect(Collectors.groupingBy(
                        Bill::getBillDescription,
                        java.util.TreeMap::new,
                        Collectors.summingDouble(Bill::getTotalAmount)));
    }

    /**
     * @return bill count grouped by payment status
     */
    public Map<String, Long> getCountByPaymentStatus() {
        return store.stream()
                .collect(Collectors.groupingBy(
                        Bill::getPaymentStatus,
                        java.util.TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * @return the largest bill raised, if any
     */
    public Optional<Bill> getHighestBill() {
        return store.stream().max(Comparator.comparingDouble(Bill::getTotalAmount));
    }

    /**
     * @return the mean bill value, or {@code 0.0} with no bills
     */
    public double getAverageBillValue() {
        return store.stream().mapToDouble(Bill::getTotalAmount).average().orElse(0.0);
    }

    public DataStore<Bill> getStore() {
        return store;
    }

    /** Prints a one-line summary of every bill. */
    public void displayAll() {
        if (store.isEmpty()) {
            System.out.println("  No bills raised yet.");
            return;
        }
        System.out.println("\n  Bills (" + store.count() + ")");
        System.out.println("  " + "-".repeat(96));
        store.findAll().forEach(Bill::displayDetails);
    }
}
