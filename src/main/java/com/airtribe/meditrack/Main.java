package com.airtribe.meditrack;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.MedicalEntity;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.DataPersistenceException;
import com.airtribe.meditrack.exception.EntityNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.exception.SlotUnavailableException;
import com.airtribe.meditrack.interfaces.Payable;
import com.airtribe.meditrack.observer.AuditLogObserver;
import com.airtribe.meditrack.observer.ConsoleReminderObserver;
import com.airtribe.meditrack.observer.SmsReminderObserver;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.NotificationService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.test.TestRunner;
import com.airtribe.meditrack.util.AIHelper;
import com.airtribe.meditrack.util.AppConfig;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.DemoDataSeeder;
import com.airtribe.meditrack.util.IdGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    private final Scanner scanner = new Scanner(System.in);
    private final AppConfig config = AppConfig.getInstance();

    private final PatientService patientService = new PatientService();
    private final DoctorService doctorService = new DoctorService();
    private final NotificationService notificationService = new NotificationService();
    private final AppointmentService appointmentService =
            new AppointmentService(new com.airtribe.meditrack.util.DataStore<>("Appointment"), notificationService);
    private final BillingService billingService = new BillingService();
    private final AuditLogObserver auditLog = new AuditLogObserver();

    private boolean running = true;

    /**
     * @param args command-line arguments; see the class documentation
     */
    public static void main(String[] args) {
        List<String> arguments = Arrays.asList(args);

        if (arguments.contains(Constants.ARG_HELP)) {
            printUsage();
            return;
        }
        if (arguments.contains(Constants.ARG_RUN_TESTS)) {
            TestRunner.main(new String[0]);
            return;
        }

        Main app = new Main();
        app.start(arguments);
    }

    private void start(List<String> arguments) {
        printBanner();
        registerObservers();

        if (arguments.contains(Constants.ARG_LOAD_DATA)) {
            loadAllData();
        }
        if (arguments.contains(Constants.ARG_SEED_DEMO)) {
            DemoDataSeeder.seed(patientService, doctorService, appointmentService);
        }
        if (config.isRemindersEnabled()) {
            notificationService.startReminderScheduler(appointmentService::getAllAppointments, 300);
        }

        runMainMenu();
        shutdown();
    }

    private void registerObservers() {
        notificationService.register(new ConsoleReminderObserver());
        notificationService.register(new SmsReminderObserver());
        notificationService.register(auditLog);
    }

    private void shutdown() {
        notificationService.stopReminderScheduler();
        System.out.println("\n  Thank you for using " + Constants.APP_NAME + ". Goodbye.\n");
        scanner.close();
    }

    private void runMainMenu() {
        while (running) {
            printMainMenu();
            switch (readLine("  Choose an option: ")) {
                case "1" -> patientMenu();
                case "2" -> doctorMenu();
                case "3" -> appointmentMenu();
                case "4" -> billingMenu();
                case "5" -> searchMenu();
                case "6" -> aiMenu();
                case "7" -> reportsMenu();
                case "8" -> dataMenu();
                case "9" -> demonstrationsMenu();
                case "0" -> running = false;
                default -> System.out.println("  Unrecognised option. Please try again.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("""

                  ============================================================
                    MAIN MENU
                  ============================================================
                    1. Patients          5. Search
                    2. Doctors           6. AI Triage
                    3. Appointments      7. Reports & Analytics
                    4. Billing           8. Data & Settings
                                         9. OOP Demonstrations
                    0. Exit
                  ============================================================""");
    }

    private void patientMenu() {
        System.out.println("""

                  --- PATIENTS ---
                    1. Add patient          4. Delete patient
                    2. List patients        5. Add medical history
                    3. Update patient       6. Add allergy
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> addPatient();
            case "2" -> patientService.displayAll();
            case "3" -> updatePatient();
            case "4" -> deletePatient();
            case "5" -> addMedicalHistory();
            case "6" -> addAllergy();
            default -> { /* back */ }
        }
    }

    private void addPatient() {
        try {
            String name = readLine("  Name: ");
            int age = Integer.parseInt(readLine("  Age: ").trim());
            String contact = readLine("  Contact number: ");
            String bloodGroup = readLine("  Blood group (optional): ");
            boolean insured = readLine("  Insured? (y/n): ").equalsIgnoreCase("y");

            Patient patient = patientService.addPatient(name, age, contact,
                    bloodGroup.isBlank() ? null : bloodGroup, insured);
            System.out.println("  Registered " + patient.getId());
            patient.displayDetails();

        } catch (NumberFormatException e) {
            System.out.println("  Age must be a whole number.");
        } catch (InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void updatePatient() {
        try {
            String id = readLine("  Patient id: ");
            String name = readLine("  New name (blank to skip): ");
            String ageText = readLine("  New age (blank to skip): ");
            String contact = readLine("  New contact (blank to skip): ");

            Integer age = ageText.isBlank() ? null : Integer.parseInt(ageText.trim());
            Patient patient = patientService.updatePatient(id,
                    name.isBlank() ? null : name, age, contact.isBlank() ? null : contact);

            System.out.println("  Updated.");
            patient.displayDetails();

        } catch (NumberFormatException e) {
            System.out.println("  Age must be a whole number.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void deletePatient() {
        String id = readLine("  Patient id to delete: ");
        System.out.println(patientService.deletePatient(id)
                ? "  Deleted " + id
                : "  No patient found with id " + id);
    }

    private void addMedicalHistory() {
        try {
            patientService.addMedicalHistory(readLine("  Patient id: "), readLine("  History note: "));
            System.out.println("  History recorded.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void addAllergy() {
        try {
            patientService.addAllergy(readLine("  Patient id: "), readLine("  Allergy: "));
            System.out.println("  Allergy recorded.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void doctorMenu() {
        System.out.println("""

                  --- DOCTORS ---
                    1. Add doctor           4. Delete doctor
                    2. List doctors         5. Rate doctor
                    3. Update doctor        6. List by speciality
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> addDoctor();
            case "2" -> doctorService.displayAll();
            case "3" -> updateDoctor();
            case "4" -> deleteDoctor();
            case "5" -> rateDoctor();
            case "6" -> listBySpeciality();
            default -> { /* back */ }
        }
    }

    private void addDoctor() {
        try {
            String name = readLine("  Name: ");
            int age = Integer.parseInt(readLine("  Age: ").trim());
            String contact = readLine("  Contact number: ");

            Specialization spec = promptSpecialization();
            if (spec == null) {
                return;
            }
            String feeText = readLine("  Consultation fee (blank for default "
                    + spec.getBaseConsultationFee() + "): ");
            double fee = feeText.isBlank() ? spec.getBaseConsultationFee() : Double.parseDouble(feeText.trim());
            int experience = Integer.parseInt(readLine("  Years of experience: ").trim());

            Doctor doctor = doctorService.addDoctor(name, age, contact, spec, fee, experience);
            System.out.println("  Added " + doctor.getId());
            doctor.displayDetails();

        } catch (NumberFormatException e) {
            System.out.println("  Age, fee and experience must be numbers.");
        } catch (InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void updateDoctor() {
        try {
            String id = readLine("  Doctor id: ");
            String name = readLine("  New name (blank to skip): ");
            String feeText = readLine("  New fee (blank to skip): ");
            String availableText = readLine("  Available? (y/n, blank to skip): ");

            Double fee = feeText.isBlank() ? null : Double.parseDouble(feeText.trim());
            Boolean available = availableText.isBlank() ? null : availableText.equalsIgnoreCase("y");

            Doctor doctor = doctorService.updateDoctor(id,
                    name.isBlank() ? null : name, fee, available);
            System.out.println("  Updated.");
            doctor.displayDetails();

        } catch (NumberFormatException e) {
            System.out.println("  Fee must be a number.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void deleteDoctor() {
        String id = readLine("  Doctor id to delete: ");
        System.out.println(doctorService.deleteDoctor(id)
                ? "  Deleted " + id
                : "  No doctor found with id " + id);
    }

    private void rateDoctor() {
        try {
            doctorService.rateDoctor(readLine("  Doctor id: "),
                    Double.parseDouble(readLine("  Rating (0-5): ").trim()));
            System.out.println("  Rating recorded.");
        } catch (NumberFormatException e) {
            System.out.println("  Rating must be a number.");
        } catch (EntityNotFoundException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void listBySpeciality() {
        Specialization spec = promptSpecialization();
        if (spec == null) {
            return;
        }
        List<Doctor> doctors = doctorService.searchDoctor(spec);
        if (doctors.isEmpty()) {
            System.out.println("  No doctors in " + spec.getDisplayName() + ".");
            return;
        }
        System.out.println("\n  " + spec.getDisplayName() + " (" + doctors.size() + ")");
        doctors.forEach(Doctor::displayDetails);
    }

    private void appointmentMenu() {
        System.out.println("""

                  --- APPOINTMENTS ---
                    1. Book appointment       5. Complete appointment
                    2. List appointments      6. Reschedule appointment
                    3. Confirm appointment    7. Upcoming appointments
                    4. Cancel appointment     8. Doctor's free slots
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> bookAppointment();
            case "2" -> appointmentService.displayAll();
            case "3" -> changeStatus("confirm");
            case "4" -> changeStatus("cancel");
            case "5" -> changeStatus("complete");
            case "6" -> rescheduleAppointment();
            case "7" -> listUpcoming();
            case "8" -> showFreeSlots();
            default -> { /* back */ }
        }
    }

    private void bookAppointment() {
        try {
            Patient patient = patientService.findById(readLine("  Patient id: ")).orElse(null);
            if (patient == null) {
                System.out.println("  No such patient.");
                return;
            }
            Doctor doctor = doctorService.findById(readLine("  Doctor id: ")).orElse(null);
            if (doctor == null) {
                System.out.println("  No such doctor.");
                return;
            }
            LocalDateTime slot = DateUtil.parseDateTime(
                    readLine("  Slot (" + Constants.DATE_TIME_FORMAT + "): "));
            List<String> symptoms = splitSymptoms(readLine("  Symptoms (comma separated, optional): "));

            Appointment appointment = appointmentService.bookAppointment(patient, doctor, slot, symptoms);
            System.out.println("  Booked " + appointment.getId());
            appointment.displayDetails();

        } catch (InvalidDataException | SlotUnavailableException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void changeStatus(String action) {
        String id = readLine("  Appointment id: ");
        try {
            Appointment appointment = switch (action) {
                case "confirm" -> appointmentService.confirmAppointment(id);
                case "cancel" -> appointmentService.cancelAppointment(id);
                default -> appointmentService.completeAppointment(id);
            };
            System.out.println("  Appointment is now " + appointment.getStatus().getDisplayName() + ".");
        } catch (AppointmentNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void rescheduleAppointment() {
        try {
            String id = readLine("  Appointment id: ");
            LocalDateTime slot = DateUtil.parseDateTime(
                    readLine("  New slot (" + Constants.DATE_TIME_FORMAT + "): "));
            Appointment appointment = appointmentService.rescheduleAppointment(id, slot);
            System.out.println("  Rescheduled.");
            appointment.displayDetails();
        } catch (AppointmentNotFoundException | InvalidDataException | SlotUnavailableException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void listUpcoming() {
        List<Appointment> upcoming = appointmentService.getUpcomingAppointments();
        if (upcoming.isEmpty()) {
            System.out.println("  No upcoming appointments.");
            return;
        }
        System.out.println("\n  Upcoming (" + upcoming.size() + ")");
        upcoming.forEach(Appointment::displayDetails);
    }

    private void showFreeSlots() {
        try {
            String doctorId = readLine("  Doctor id: ");
            LocalDate date = DateUtil.parseDate(readLine("  Date (" + Constants.DATE_FORMAT + "): "));
            List<LocalDateTime> slots = appointmentService.getAvailableSlots(doctorId, date);

            if (slots.isEmpty()) {
                System.out.println("  No free slots on that day.");
                return;
            }
            System.out.println("  Free slots (" + slots.size() + "):");
            slots.forEach(slot -> System.out.println("    " + DateUtil.formatForDisplay(slot)));

        } catch (InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void billingMenu() {
        System.out.println("""

                  --- BILLING ---
                    1. Generate bill for appointment    4. List bills
                    2. Record payment                   5. Unpaid bills
                    3. Print bill                       6. Compare billing strategies
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> generateBill();
            case "2" -> recordPayment();
            case "3" -> printBill();
            case "4" -> billingService.displayAll();
            case "5" -> listUnpaid();
            case "6" -> compareStrategies();
            default -> { /* back */ }
        }
    }

    private void generateBill() {
        try {
            Appointment appointment = appointmentService.requireAppointment(readLine("  Appointment id: "));
            System.out.println("  Bill types: 1) Consultation  2) Procedure  3) Emergency");
            BillType type = switch (readLine("  Type: ")) {
                case "2" -> BillType.PROCEDURE;
                case "3" -> BillType.EMERGENCY;
                default -> BillType.CONSULTATION;
            };

            Bill bill = billingService.generateBillForAppointment(appointment, type);
            System.out.println(bill.getFormattedBill());

        } catch (AppointmentNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void recordPayment() {
        try {
            String billId = readLine("  Bill id: ");
            double amount = Double.parseDouble(readLine("  Amount: ").trim());
            Bill bill = billingService.recordPayment(billId, amount);
            System.out.printf("  Payment accepted. Balance due: %s (%s)%n",
                    Payable.formatCurrency(bill.getAmountDue()), bill.getPaymentStatus());
        } catch (NumberFormatException e) {
            System.out.println("  Amount must be a number.");
        } catch (EntityNotFoundException | InvalidDataException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private void printBill() {
        billingService.findById(readLine("  Bill id: "))
                .ifPresentOrElse(
                        bill -> System.out.println(bill.getFormattedBill()),
                        () -> System.out.println("  No such bill."));
    }

    private void listUnpaid() {
        List<Bill> unpaid = billingService.getUnpaidBills();
        if (unpaid.isEmpty()) {
            System.out.println("  Every bill is settled.");
            return;
        }
        System.out.println("\n  Unpaid bills (" + unpaid.size() + ")");
        unpaid.forEach(Bill::displayDetails);
    }

    /** Shows the same charge priced three ways — the Strategy pattern, visibly. */
    private void compareStrategies() {
        Optional<Patient> any = patientService.getAllPatients().stream().findFirst();
        if (any.isEmpty()) {
            System.out.println("  Register a patient first.");
            return;
        }
        Patient patient = any.get();
        double baseFee = 1000.0;

        System.out.println("\n  Same " + Payable.formatCurrency(baseFee)
                + " consultation for " + patient.getName() + ", priced three ways:\n");

        List<com.airtribe.meditrack.interfaces.BillingStrategy> strategies = List.of(
                new com.airtribe.meditrack.strategy.StandardBillingStrategy(),
                new com.airtribe.meditrack.strategy.InsuranceBillingStrategy(),
                new com.airtribe.meditrack.strategy.SeniorCitizenBillingStrategy());

        for (var strategy : strategies) {
            try {
                Bill bill = billingService.generateBillWithStrategy(
                        patient, BillType.CONSULTATION, baseFee, strategy);
                System.out.printf("    %-28s total %12s   — %s%n",
                        strategy.getStrategyName(),
                        Payable.formatCurrency(bill.getTotalAmount()),
                        strategy.describe());
            } catch (InvalidDataException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private void searchMenu() {
        System.out.println("""

                  --- SEARCH ---
                    1. Patients by keyword     4. Doctors by speciality
                    2. Patients by age         5. Doctors under a fee
                    3. Patients by age range   6. Appointments by keyword
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> printPatients(patientService.searchPatient(readLine("  Keyword: ")));
            case "2" -> searchPatientsByAge();
            case "3" -> searchPatientsByAgeRange();
            case "4" -> searchDoctorsBySpeciality();
            case "5" -> searchDoctorsByFee();
            case "6" -> printAppointments(appointmentService.searchAppointments(readLine("  Keyword: ")));
            default -> { /* back */ }
        }
    }

    private void searchPatientsByAge() {
        try {
            printPatients(patientService.searchPatient(Integer.parseInt(readLine("  Age: ").trim())));
        } catch (NumberFormatException e) {
            System.out.println("  Age must be a whole number.");
        }
    }

    private void searchPatientsByAgeRange() {
        try {
            int min = Integer.parseInt(readLine("  Minimum age: ").trim());
            int max = Integer.parseInt(readLine("  Maximum age: ").trim());
            printPatients(patientService.searchPatient(min, max));
        } catch (NumberFormatException e) {
            System.out.println("  Ages must be whole numbers.");
        }
    }

    private void searchDoctorsBySpeciality() {
        Specialization spec = promptSpecialization();
        if (spec != null) {
            printDoctors(doctorService.searchDoctor(spec));
        }
    }

    private void searchDoctorsByFee() {
        try {
            printDoctors(doctorService.searchDoctor(Double.parseDouble(readLine("  Maximum fee: ").trim())));
        } catch (NumberFormatException e) {
            System.out.println("  Fee must be a number.");
        }
    }

    private void aiMenu() {
        System.out.println("""

                  --- AI TRIAGE ---
                    1. Recommend a doctor from symptoms
                    2. Suggest appointment slots
                    3. Screening prompts for a patient
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> runTriage();
            case "2" -> suggestSlots();
            case "3" -> screeningPrompts();
            default -> { /* back */ }
        }
    }

    private void runTriage() {
        String symptoms = readLine("  Describe the symptoms: ");
        System.out.println(AIHelper.buildTriageReport(symptoms,
                doctorService.getAllDoctors(), appointmentService.getAllAppointments()));
    }

    private void suggestSlots() {
        doctorService.findById(readLine("  Doctor id: ")).ifPresentOrElse(doctor -> {
            List<LocalDateTime> slots = AIHelper.suggestSlots(
                    doctor, appointmentService.getAllAppointments(), 5, 7);
            if (slots.isEmpty()) {
                System.out.println("  No slots free in the next 7 days.");
                return;
            }
            System.out.println("  Suggested slots for Dr. " + doctor.getName() + ":");
            slots.forEach(slot -> System.out.println("    " + DateUtil.formatForDisplay(slot)));
        }, () -> System.out.println("  No such doctor."));
    }

    private void screeningPrompts() {
        patientService.findById(readLine("  Patient id: ")).ifPresentOrElse(patient -> {
            List<String> prompts = AIHelper.suggestScreeningPrompts(patient);
            if (prompts.isEmpty()) {
                System.out.println("  Nothing to flag — this record is complete.");
                return;
            }
            System.out.println("  Screening prompts for " + patient.getName() + ":");
            prompts.forEach(prompt -> System.out.println("    - " + prompt));
        }, () -> System.out.println("  No such patient."));
    }

    private void reportsMenu() {
        System.out.println("""

                  --- REPORTS & ANALYTICS (Java Streams) ---
                    1. Appointments per doctor      5. Patient demographics
                    2. Average fee by speciality    6. Revenue summary
                    3. Appointment status breakdown 7. Audit trail
                    4. Top rated doctors            8. Notification channels
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> reportAppointmentsPerDoctor();
            case "2" -> reportAverageFees();
            case "3" -> reportStatusBreakdown();
            case "4" -> printDoctors(doctorService.getTopRatedDoctors(3));
            case "5" -> reportDemographics();
            case "6" -> reportRevenue();
            case "7" -> auditLog.printTrail();
            case "8" -> notificationService.printObservers();
            default -> { /* back */ }
        }
    }

    private void reportAppointmentsPerDoctor() {
        var perDoctor = appointmentService.getAppointmentsPerDoctor();
        if (perDoctor.isEmpty()) {
            System.out.println("  No appointments recorded.");
            return;
        }
        System.out.println("\n  Appointments per doctor");
        perDoctor.forEach((name, count) -> System.out.printf("    %-24s %d%n", name, count));
        appointmentService.getBusiestDoctor().ifPresent(entry ->
                System.out.printf("    -> Busiest: %s (%d)%n", entry.getKey(), entry.getValue()));
    }

    private void reportAverageFees() {
        var averages = doctorService.getAverageFeeBySpecialization();
        if (averages.isEmpty()) {
            System.out.println("  No doctors on the roster.");
            return;
        }
        System.out.println("\n  Average consultation fee by speciality");
        averages.forEach((spec, avg) ->
                System.out.printf("    %-20s %12s%n", spec.getDisplayName(), Payable.formatCurrency(avg)));

        var stats = doctorService.getFeeStatistics();
        System.out.printf("    %-20s %12s  (min %s, max %s across %d doctors)%n",
                "OVERALL", Payable.formatCurrency(stats.getAverage()),
                Payable.formatCurrency(stats.getMin()),
                Payable.formatCurrency(stats.getMax()), stats.getCount());

        List<Specialization> gaps = doctorService.getUnstaffedSpecializations();
        if (!gaps.isEmpty()) {
            System.out.println("    Unstaffed: " + gaps.stream()
                    .map(Specialization::getDisplayName).toList());
        }
    }

    private void reportStatusBreakdown() {
        var breakdown = appointmentService.getStatusBreakdown();
        if (breakdown.isEmpty()) {
            System.out.println("  No appointments recorded.");
            return;
        }
        System.out.println("\n  Appointment status breakdown");
        breakdown.forEach((status, count) ->
                System.out.printf("    %-12s %d%n", status.getDisplayName(), count));
        System.out.printf("    Cancellation rate: %.1f%%%n",
                appointmentService.getCancellationRate() * 100);
    }

    private void reportDemographics() {
        System.out.println("\n  Patient demographics");
        patientService.countByAgeGroup().forEach((group, count) ->
                System.out.printf("    %-10s %d%n", group, count));
        System.out.printf("    Average age    : %.1f%n", patientService.getAverageAge());
        System.out.printf("    Senior citizens: %d%n", patientService.getSeniorCitizens().size());
        System.out.printf("    Insured        : %d%n", patientService.getInsuredPatients().size());

        List<String> allergies = patientService.getAllKnownAllergies();
        if (!allergies.isEmpty()) {
            System.out.println("    Known allergies: " + String.join(", ", allergies));
        }
    }

    private void reportRevenue() {
        System.out.println("\n  Revenue summary");
        System.out.printf("    Total billed      : %s%n",
                Payable.formatCurrency(billingService.getTotalBilled()));
        System.out.printf("    Total collected   : %s%n",
                Payable.formatCurrency(billingService.getTotalCollected()));
        System.out.printf("    Outstanding       : %s%n",
                Payable.formatCurrency(billingService.getTotalOutstanding()));
        System.out.printf("    GST collected     : %s%n",
                Payable.formatCurrency(billingService.getTotalTaxCollected()));
        System.out.printf("    Average bill value: %s%n",
                Payable.formatCurrency(billingService.getAverageBillValue()));

        var byType = billingService.getRevenueByBillType();
        if (!byType.isEmpty()) {
            System.out.println("    By bill type:");
            byType.forEach((type, total) ->
                    System.out.printf("      %-20s %s%n", type, Payable.formatCurrency(total)));
        }
    }

    private void dataMenu() {
        System.out.println("""

                  --- DATA & SETTINGS ---
                    1. Save all to CSV        4. Show configuration
                    2. Load all from CSV      5. Toggle reminders
                    3. Seed demo data         6. Show id counters
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> saveAllData();
            case "2" -> loadAllData();
            case "3" -> DemoDataSeeder.seed(patientService, doctorService, appointmentService);
            case "4" -> config.printConfiguration();
            case "5" -> toggleReminders();
            case "6" -> IdGenerator.getInstance().printCounters();
            default -> { /* back */ }
        }
    }

    private void saveAllData() {
        try {
            patientService.saveToFile();
            doctorService.saveToFile();
            appointmentService.saveToFile();
            System.out.printf("  Saved %d patients, %d doctors, %d appointments to %s/%n",
                    patientService.count(), doctorService.count(),
                    appointmentService.count(), Constants.DATA_DIR);
        } catch (DataPersistenceException e) {
            System.out.println("  Save failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Caused by: " + e.getCause());
            }
        }
    }

    private void loadAllData() {
        try {
            int doctors = doctorService.loadFromFile();
            int patients = patientService.loadFromFile();
            int appointments = appointmentService.loadFromFile(
                    patientService.asMap(), doctorService.asMap());

            System.out.printf("  Loaded %d patients, %d doctors, %d appointments from %s/%n",
                    patients, doctors, appointments, Constants.DATA_DIR);

        } catch (DataPersistenceException e) {
            System.out.println("  Load failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Caused by: " + e.getCause());
            }
        }
    }

    private void toggleReminders() {
        boolean enable = !notificationService.isSchedulerRunning();
        if (enable) {
            notificationService.startReminderScheduler(appointmentService::getAllAppointments, 300);
        } else {
            notificationService.stopReminderScheduler();
        }
    }

    private void demonstrationsMenu() {
        System.out.println("""

                  --- OOP DEMONSTRATIONS ---
                    1. Dynamic dispatch (polymorphism)
                    2. Deep vs shallow copy
                    3. Immutability of BillSummary
                    4. Singleton identity (eager vs lazy)
                    0. Back""");

        switch (readLine("  Choose: ")) {
            case "1" -> demoDynamicDispatch();
            case "2" -> demoCopySemantics();
            case "3" -> demoImmutability();
            case "4" -> demoSingletons();
            default -> { /* back */ }
        }
    }

    /** One loop, one call, three different behaviours — resolved at run time. */
    private void demoDynamicDispatch() {
        List<MedicalEntity> entities = new ArrayList<>();
        entities.addAll(patientService.getAllPatients());
        entities.addAll(doctorService.getAllDoctors());
        entities.addAll(appointmentService.getAllAppointments());

        if (entities.isEmpty()) {
            System.out.println("  Seed some data first (Data & Settings -> 3).");
            return;
        }
        System.out.println("\n  Iterating List<MedicalEntity> and calling displayDetails() on each.");
        System.out.println("  The reference type is identical; the JVM picks the override at run time.\n");
        entities.forEach(MedicalEntity::displayDetails);
        System.out.println("\n  Total entities created this session: "
                + MedicalEntity.getTotalEntitiesCreated());
    }

    private void demoCopySemantics() {
        Optional<Patient> first = patientService.getAllPatients().stream().findFirst();
        if (first.isEmpty()) {
            System.out.println("  Register a patient first.");
            return;
        }
        try {
            Patient original = first.get();
            Patient deep = original.clone();
            Patient shallow = original.shallowCopy();

            System.out.println("\n  Original history : " + original.getMedicalHistory());
            original.addMedicalHistoryEntry("MUTATION-AFTER-COPY");

            System.out.println("  After mutating the ORIGINAL:");
            System.out.println("    original : " + original.getMedicalHistory());
            System.out.println("    deep copy: " + deep.getMedicalHistory() + "   <- unaffected");
            System.out.println("    shallow  : " + shallow.getMedicalHistory() + "   <- changed too");
            System.out.println("\n  The shallow copy shares the original's list; the deep copy owns its own.");

        } catch (CloneNotSupportedException e) {
            System.out.println("  Clone failed: " + e.getMessage());
        }
    }

    private void demoImmutability() {
        List<Bill> bills = billingService.getAllBills();
        if (bills.isEmpty()) {
            System.out.println("  Generate a bill first (Billing -> 1).");
            return;
        }
        var summary = bills.get(0).toSummary();
        System.out.println("\n  BillSummary: " + summary);
        System.out.println("  Attempting to mutate its line-item list...");
        try {
            summary.getLineItems().add(new Bill.LineItem("Injected charge", 9999));
            System.out.println("  Mutation succeeded — that would be a bug.");
        } catch (UnsupportedOperationException e) {
            System.out.println("  Rejected with UnsupportedOperationException — the list is unmodifiable.");
        }
        var withPayment = summary.withPayment(100);
        System.out.println("  withPayment(100) returns a NEW object: "
                + (summary != withPayment ? "confirmed" : "same reference (bug)"));
        System.out.println("  Original paid: " + summary.getAmountPaid()
                + ", new object paid: " + withPayment.getAmountPaid());
    }

    private void demoSingletons() {
        System.out.println("\n  AppConfig (eager) — same instance twice? "
                + (AppConfig.getInstance() == AppConfig.getInstance()));
        System.out.println("  IdGenerator (lazy) — same instance twice? "
                + (IdGenerator.getInstance() == IdGenerator.getInstance()));
        System.out.println("  AppConfig identity hash : " + System.identityHashCode(AppConfig.getInstance()));
        System.out.println("  IdGenerator identity hash: " + System.identityHashCode(IdGenerator.getInstance()));
        IdGenerator.getInstance().printCounters();
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
    }

    private Specialization promptSpecialization() {
        Specialization[] values = Specialization.values();
        System.out.println("  Specialities:");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("    %d) %s%n", i + 1, values[i].getDisplayName());
        }
        try {
            int choice = Integer.parseInt(readLine("  Choose: ").trim());
            if (choice < 1 || choice > values.length) {
                System.out.println("  Out of range.");
                return null;
            }
            return values[choice - 1];
        } catch (NumberFormatException e) {
            System.out.println("  Please enter a number.");
            return null;
        }
    }

    private static List<String> splitSymptoms(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private void printPatients(List<Patient> patients) {
        if (patients.isEmpty()) {
            System.out.println("  No matches.");
            return;
        }
        System.out.println("  " + patients.size() + " match(es):");
        patients.forEach(Patient::displayDetails);
    }

    private void printDoctors(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            System.out.println("  No matches.");
            return;
        }
        System.out.println("  " + doctors.size() + " match(es):");
        doctors.forEach(Doctor::displayDetails);
    }

    private void printAppointments(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            System.out.println("  No matches.");
            return;
        }
        System.out.println("  " + appointments.size() + " match(es):");
        appointments.forEach(Appointment::displayDetails);
    }

    private void printBanner() {
        System.out.println("""

                  ============================================================
                     __  __          _ _ _____              _
                    |  \\/  | ___  __| (_)_   _| __ __ _  ___| | __
                    | |\\/| |/ _ \\/ _` | | | || '__/ _` |/ __| |/ /
                    | |  | |  __/ (_| | | | || | | (_| | (__|   <
                    |_|  |_|\\___|\\__,_|_| |_||_|  \\__,_|\\___|_|\\_\\
                  ============================================================""");
        System.out.printf("    %s v%s — %s%n", Constants.APP_NAME, Constants.APP_VERSION, Constants.ORG_NAME);
        System.out.printf("    Java %s on %s%n",
                System.getProperty("java.version"), System.getProperty("os.name"));
        System.out.println("  ============================================================");
    }

    private static void printUsage() {
        System.out.printf("""

                %s v%s — Clinic & Appointment Management System

                Usage:
                  java -cp out com.airtribe.meditrack.Main [options]

                Options:
                  %-12s  Restore patients, doctors and appointments from %s/
                  %-12s  Populate an in-memory demo clinic
                  %-12s  Run the manual test suite and exit
                  %-12s  Show this message

                Examples:
                  java -cp out com.airtribe.meditrack.Main --seedDemo
                  java -cp out com.airtribe.meditrack.Main --loadData
                  java -cp out com.airtribe.meditrack.Main --runTests
                %n""",
                Constants.APP_NAME, Constants.APP_VERSION,
                Constants.ARG_LOAD_DATA, Constants.DATA_DIR,
                Constants.ARG_SEED_DEMO,
                Constants.ARG_RUN_TESTS,
                Constants.ARG_HELP);
    }
}
