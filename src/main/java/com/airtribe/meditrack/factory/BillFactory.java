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

public final class BillFactory {

    private BillFactory() {
        throw new AssertionError("BillFactory is a static factory and must not be instantiated.");
    }

    public static Bill createBill(BillType type, Patient patient, String appointmentId, double baseFee) {
        return createBill(type, patient, appointmentId, baseFee, chooseStrategy(patient));
    }

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

    public static Bill createBillForAppointment(BillType type, Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Cannot bill a null appointment.");
        }
        return createBill(type,
                appointment.getPatient(),
                appointment.getId(),
                appointment.getConsultationFee());
    }

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

    private static boolean isFirstVisit(Patient patient) {
        return patient != null && patient.getMedicalHistory().isEmpty();
    }

    /** Emergency attendance outside clinic hours attracts a doubled surcharge. */
    private static boolean isAfterHours() {
        return !DateUtil.isWithinClinicHours(java.time.LocalDateTime.now());
    }
}
