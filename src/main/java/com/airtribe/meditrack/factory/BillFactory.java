package com.airtribe.meditrack.factory;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.ConsultationBill;
import com.airtribe.meditrack.entity.EmergencyBill;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.ProcedureBill;
import com.airtribe.meditrack.interfaces.BillingStrategy;
import com.airtribe.meditrack.strategy.InsuranceBillingStrategy;
import com.airtribe.meditrack.strategy.SeniorCitizenBillingStrategy;
import com.airtribe.meditrack.strategy.StandardBillingStrategy;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.IdGenerator;

/**
 * <b>Factory Pattern</b> — the one place that decides which {@link Bill} subclass to build.
 *
 * <h2>What the factory buys us</h2>
 * <p>Without it, every caller wanting a bill would need to know the full subclass
 * catalogue, remember to fetch an id from {@link IdGenerator}, and pick a pricing
 * strategy. Adding a fourth bill type would mean editing every one of those call sites.</p>
 *
 * <p>With it, callers ask for a {@link BillType} and receive a {@code Bill}. They are
 * coupled to the <em>abstraction</em>, not the concrete classes — the Dependency
 * Inversion Principle — and new bill types are added by editing this class alone, which
 * is the Open/Closed Principle from the other side.</p>
 *
 * <p>The factory also composes with the Strategy pattern: {@link #chooseStrategy(Patient)}
 * picks a pricing policy from the patient's own attributes, so the two patterns cooperate
 * rather than duplicating each other's decisions.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public final class BillFactory {

    private BillFactory() {
        throw new AssertionError("BillFactory is a static factory and must not be instantiated.");
    }

    /**
     * Builds a bill of the requested type, choosing a pricing strategy automatically.
     *
     * @param type          which kind of bill to create
     * @param patient       who is being billed
     * @param appointmentId the appointment being settled, may be {@code null}
     * @param baseFee       the consultation or attendance fee
     * @return a concrete {@link Bill}, never {@code null}
     */
    public static Bill createBill(BillType type, Patient patient, String appointmentId, double baseFee) {
        return createBill(type, patient, appointmentId, baseFee, chooseStrategy(patient));
    }

    /**
     * Builds a bill of the requested type with an explicit pricing strategy.
     *
     * <p>The {@code switch} is exhaustive over {@link BillType}; adding a constant without
     * handling it here is a compile-time error, not a run-time surprise.</p>
     *
     * @param type          which kind of bill to create
     * @param patient       who is being billed
     * @param appointmentId the appointment being settled, may be {@code null}
     * @param baseFee       the consultation or attendance fee
     * @param strategy      the pricing policy to apply
     * @return a concrete {@link Bill}, never {@code null}
     */
    public static Bill createBill(BillType type, Patient patient, String appointmentId,
                                  double baseFee, BillingStrategy strategy) {
        String billId = IdGenerator.getInstance().nextBillId();
        BillType resolved = type == null ? BillType.CONSULTATION : type;

        return switch (resolved) {
            case CONSULTATION -> new ConsultationBill(
                    billId, patient, appointmentId, baseFee, isFirstVisit(patient), strategy);

            case PROCEDURE -> new ProcedureBill(
                    billId, patient, appointmentId, baseFee, strategy);

            case EMERGENCY -> new EmergencyBill(
                    billId, patient, appointmentId, baseFee, isAfterHours(), strategy);
        };
    }

    /**
     * Convenience overload that derives everything it can from an appointment.
     *
     * @param type        which kind of bill to create
     * @param appointment the completed appointment to bill for
     * @return a concrete {@link Bill}
     */
    public static Bill createBillForAppointment(BillType type, Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Cannot bill a null appointment.");
        }
        return createBill(type,
                appointment.getPatient(),
                appointment.getId(),
                appointment.getConsultationFee());
    }

    /**
     * Picks a pricing policy from the patient's own attributes.
     *
     * <p>Precedence is deliberate: insurance is checked first because it is a contractual
     * arrangement, and the senior concession applies only to self-paying patients.</p>
     *
     * @param patient the patient being billed, may be {@code null}
     * @return the strategy to apply
     */
    public static BillingStrategy chooseStrategy(Patient patient) {
        if (patient == null) {
            return new StandardBillingStrategy();
        }
        if (patient.isInsured()) {
            return new InsuranceBillingStrategy();
        }
        if (patient.isSeniorCitizen()) {
            return new SeniorCitizenBillingStrategy();
        }
        return new StandardBillingStrategy();
    }

    /**
     * A patient with no recorded history is treated as a first visit, which triggers the
     * registration fee on a {@link ConsultationBill}.
     */
    private static boolean isFirstVisit(Patient patient) {
        return patient != null && patient.getMedicalHistory().isEmpty();
    }

    /** Emergency attendance outside clinic hours attracts a doubled surcharge. */
    private static boolean isAfterHours() {
        return !DateUtil.isWithinClinicHours(java.time.LocalDateTime.now());
    }
}
