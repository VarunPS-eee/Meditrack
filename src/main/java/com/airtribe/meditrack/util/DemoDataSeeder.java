package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.exception.SlotUnavailableException;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Populates the application with a realistic demo dataset.
 *
 * <p>Exists so a reviewer running {@code --seedDemo} sees a working clinic immediately
 * rather than a set of empty menus. Also used by the walkthrough in the README.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class DemoDataSeeder {

    private DemoDataSeeder() {
        throw new AssertionError("DemoDataSeeder is a utility class and must not be instantiated.");
    }

    /**
     * Seeds doctors, patients and a few appointments.
     *
     * @param patientService     patient store to populate
     * @param doctorService      doctor store to populate
     * @param appointmentService appointment store to populate
     */
    public static void seed(PatientService patientService,
                            DoctorService doctorService,
                            AppointmentService appointmentService) {
        try {
            seedDoctors(doctorService);
            seedPatients(patientService);
            seedAppointments(patientService, doctorService, appointmentService);
            System.out.printf("  Demo data seeded: %d doctors, %d patients, %d appointments.%n",
                    doctorService.count(), patientService.count(), appointmentService.count());
        } catch (InvalidDataException e) {
            System.out.println("  [WARN] Demo seeding failed validation: " + e.getMessage());
        }
    }

    private static void seedDoctors(DoctorService service) throws InvalidDataException {
        service.addDoctor("Anita Rao", 44, "9876543210", Specialization.CARDIOLOGY, 1400, 16);
        service.addDoctor("Vikram Nair", 38, "9876543211", Specialization.NEUROLOGY, 1600, 11);
        service.addDoctor("Priya Sharma", 33, "9876543212", Specialization.DERMATOLOGY, 850, 6);
        service.addDoctor("Rahul Mehta", 51, "9876543213", Specialization.ORTHOPEDICS, 1100, 22);
        service.addDoctor("Sneha Iyer", 36, "9876543214", Specialization.PEDIATRICS, 750, 9);
        service.addDoctor("Arjun Desai", 29, "9876543215", Specialization.GENERAL_PRACTICE, 500, 3);

        // Seed some ratings so the AI ranking has something to work with.
        try {
            service.rateDoctor("DOC-0001", 4.8);
            service.rateDoctor("DOC-0002", 4.5);
            service.rateDoctor("DOC-0003", 4.9);
            service.rateDoctor("DOC-0004", 4.2);
            service.rateDoctor("DOC-0005", 4.7);
            service.rateDoctor("DOC-0006", 4.0);
        } catch (Exception e) {
            // Ratings are cosmetic for the demo; a miss here must not abort seeding.
        }
    }

    private static void seedPatients(PatientService service) throws InvalidDataException {
        Patient ravi = service.addPatient("Ravi Kumar", 67, "9812345670", "O+", false);
        ravi.addMedicalHistoryEntry("Hypertension diagnosed 2021");
        ravi.addAllergy("Penicillin");

        Patient meera = service.addPatient("Meera Joshi", 34, "9812345671", "A+", true);
        meera.addMedicalHistoryEntry("Migraine, recurring");

        Patient aditya = service.addPatient("Aditya Verma", 8, "9812345672", "B+", true);
        aditya.addAllergy("Peanuts");

        service.addPatient("Fatima Sheikh", 45, "9812345673", "AB+", false);
        service.addPatient("Karan Singh", 72, "9812345674", "O-", false);
    }

    private static void seedAppointments(PatientService patientService,
                                         DoctorService doctorService,
                                         AppointmentService appointmentService) {
        LocalDateTime base = DateUtil.nowAlignedToSlot().plusDays(1).withHour(10).withMinute(0);

        book(appointmentService, patientService, doctorService,
                "PAT-0001", "DOC-0001", base, List.of("chest pain", "breathless"));
        book(appointmentService, patientService, doctorService,
                "PAT-0002", "DOC-0002", base.plusHours(1), List.of("headache", "migraine"));
        book(appointmentService, patientService, doctorService,
                "PAT-0003", "DOC-0005", base.plusDays(1).withHour(11), List.of("fever", "child"));
        book(appointmentService, patientService, doctorService,
                "PAT-0004", "DOC-0003", base.plusDays(1).withHour(14), List.of("skin", "rash"));
    }

    /** Books one demo appointment, swallowing failures so a clash cannot abort seeding. */
    private static void book(AppointmentService appointmentService,
                             PatientService patientService,
                             DoctorService doctorService,
                             String patientId, String doctorId,
                             LocalDateTime slot, List<String> symptoms) {
        try {
            Patient patient = patientService.findById(patientId).orElse(null);
            Doctor doctor = doctorService.findById(doctorId).orElse(null);
            if (patient != null && doctor != null) {
                appointmentService.bookAppointment(patient, doctor, slot, symptoms);
            }
        } catch (InvalidDataException | SlotUnavailableException e) {
            System.out.println("  [WARN] Demo appointment skipped: " + e.getMessage());
        }
    }
}
