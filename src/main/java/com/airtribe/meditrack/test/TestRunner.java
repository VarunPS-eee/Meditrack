package com.airtribe.meditrack.test;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillSummary;
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
import com.airtribe.meditrack.factory.BillFactory;
import com.airtribe.meditrack.interfaces.Payable;
import com.airtribe.meditrack.observer.AuditLogObserver;
import com.airtribe.meditrack.observer.SmsReminderObserver;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.NotificationService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.strategy.InsuranceBillingStrategy;
import com.airtribe.meditrack.strategy.SeniorCitizenBillingStrategy;
import com.airtribe.meditrack.strategy.StandardBillingStrategy;
import com.airtribe.meditrack.util.AIHelper;
import com.airtribe.meditrack.util.AppConfig;
import com.airtribe.meditrack.util.CSVUtil;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Hand-rolled test runner — no JUnit, as the assignment requires.
 *
 * <p>Implements the minimum a test framework actually needs: named assertions, a
 * pass/fail tally, exception capture so one failure does not stop the run, and a summary
 * with a non-zero exit code on failure so CI or Docker can gate on it.</p>
 *
 * <p>Run with {@code java -cp out com.airtribe.meditrack.test.TestRunner} or
 * {@code java -cp out com.airtribe.meditrack.Main --runTests}.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public class TestRunner {

    private static int passed;
    private static int failed;
    private static final List<String> FAILURES = new ArrayList<>();
    private static String currentSuite = "General";

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        printHeader();

        guard("Validator", TestRunner::runValidatorTests);
        guard("DateUtil", TestRunner::runDateUtilTests);
        guard("Entities", TestRunner::runEntityTests);
        guard("Copy semantics", TestRunner::runCopySemanticsTests);
        guard("Immutability", TestRunner::runImmutabilityTests);
        guard("Enums", TestRunner::runEnumTests);
        guard("Singletons", TestRunner::runSingletonTests);
        guard("DataStore", TestRunner::runDataStoreTests);
        guard("Generics and iterator", TestRunner::runGenericsAndIteratorTests);
        guard("PatientService", TestRunner::runPatientServiceTests);
        guard("DoctorService", TestRunner::runDoctorServiceTests);
        guard("AppointmentService", TestRunner::runAppointmentServiceTests);
        guard("Billing and factory", TestRunner::runBillingAndFactoryTests);
        guard("Strategies", TestRunner::runStrategyTests);
        guard("Observers", TestRunner::runObserverTests);
        guard("AI helper", TestRunner::runAIHelperTests);
        guard("Exceptions", TestRunner::runExceptionTests);
        guard("CSV", TestRunner::runCSVTests);
        guard("Concurrency", TestRunner::runConcurrencyTests);
        guard("Streams", TestRunner::runStreamsTests);

        printSummary();
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ==================================================================== suites

    private static void runValidatorTests() {
        suite("Validator — centralised validation");

        assertNoThrow("valid name accepted", () -> Validator.validateName("Ravi Kumar"));
        assertThrows("blank name rejected", InvalidDataException.class,
                () -> Validator.validateName("   "));
        assertThrows("numeric name rejected", InvalidDataException.class,
                () -> Validator.validateName("R2D2"));
        assertThrows("single-character name rejected", InvalidDataException.class,
                () -> Validator.validateName("A"));

        assertNoThrow("age 0 accepted", () -> Validator.validateAge(0));
        assertNoThrow("age 120 accepted", () -> Validator.validateAge(120));
        assertThrows("negative age rejected", InvalidDataException.class,
                () -> Validator.validateAge(-1));
        assertThrows("age 121 rejected", InvalidDataException.class,
                () -> Validator.validateAge(121));
        assertThrows("non-numeric age rejected", InvalidDataException.class,
                () -> Validator.validateAge("abc"));

        assertEquals("contact number normalised from +91 form",
                "9876543210", tryGet(() -> Validator.validateContactNumber("+91 98765-43210")));
        assertThrows("9-digit contact rejected", InvalidDataException.class,
                () -> Validator.validateContactNumber("987654321"));
        assertThrows("contact starting with 5 rejected", InvalidDataException.class,
                () -> Validator.validateContactNumber("5876543210"));

        // validateEmail(null) legitimately returns null, so assert on the value, not on
        // assertNoThrow — which treats a null return as a failure.
        assertEquals("null email accepted and returns null (optional field)",
                null, tryGet(() -> Validator.validateEmail(null)));
        assertEquals("valid email trimmed and returned", "a@b.com",
                tryGet(() -> Validator.validateEmail("  a@b.com  ")));
        assertThrows("malformed email rejected", InvalidDataException.class,
                () -> Validator.validateEmail("not-an-email"));

        assertEquals("blood group upper-cased", "AB+",
                tryGet(() -> Validator.validateBloodGroup("ab+")));
        assertThrows("invalid blood group rejected", InvalidDataException.class,
                () -> Validator.validateBloodGroup("C+"));

        assertThrows("negative fee rejected", InvalidDataException.class,
                () -> Validator.validateFee(-100, "fee"));

        // Exception chaining — the original NumberFormatException must survive.
        try {
            Validator.validateAge("abc");
            fail("expected InvalidDataException");
        } catch (InvalidDataException e) {
            assertTrue("chained cause is NumberFormatException",
                    e.getCause() instanceof NumberFormatException);
            assertEquals("field name captured", "age", e.getFieldName());
        }
    }

    private static void runDateUtilTests() {
        suite("DateUtil — java.time helpers");

        LocalDateTime slot = LocalDateTime.of(2030, 6, 15, 10, 47);
        assertEquals("slot aligned down to 30-minute boundary",
                LocalDateTime.of(2030, 6, 15, 10, 30), DateUtil.alignToSlot(slot));

        assertTrue("10:00 is within clinic hours",
                DateUtil.isWithinClinicHours(LocalDateTime.of(2030, 6, 15, 10, 0)));
        assertFalse("06:00 is outside clinic hours",
                DateUtil.isWithinClinicHours(LocalDateTime.of(2030, 6, 15, 6, 0)));
        assertFalse("18:00 is outside clinic hours",
                DateUtil.isWithinClinicHours(LocalDateTime.of(2030, 6, 15, 18, 0)));

        assertTrue("future slot detected", DateUtil.isFuture(LocalDateTime.now().plusDays(1)));
        assertFalse("past slot detected", DateUtil.isFuture(LocalDateTime.now().minusDays(1)));

        int expectedSlots = (Constants.CLINIC_CLOSE_HOUR - Constants.CLINIC_OPEN_HOUR)
                * (60 / Constants.SLOT_DURATION_MINUTES);
        assertEquals("full day generates the expected slot count", expectedSlots,
                DateUtil.generateSlotsForDay(LocalDateTime.now().toLocalDate()).size());

        assertEquals("null formats as N/A", "N/A", DateUtil.format(null));
        assertThrows("malformed date rejected", InvalidDataException.class,
                () -> DateUtil.parseDateTime("15/06/2030"));
        assertEquals("lenient parse returns null instead of throwing",
                null, DateUtil.parseDateTimeOrNull("garbage"));

        assertNoThrow("round-trip format then parse", () -> {
            LocalDateTime original = LocalDateTime.of(2030, 6, 15, 14, 30);
            return DateUtil.parseDateTime(DateUtil.format(original)).equals(original) ? "ok" : null;
        });
    }

    private static void runEntityTests() {
        suite("Entities — inheritance, encapsulation, identity");

        Patient patient = new Patient("PAT-9001", "Test Patient", 65, "9876543210", "asthma");
        Doctor doctor = new Doctor("DOC-9001", "Test Doctor", 45, "9876543211",
                Specialization.CARDIOLOGY, 1200, 15, 4.5);

        assertTrue("Patient is a Person", patient instanceof com.airtribe.meditrack.entity.Person);
        assertTrue("Patient is a MedicalEntity", patient instanceof MedicalEntity);
        assertTrue("Doctor is a MedicalEntity", doctor instanceof MedicalEntity);

        assertEquals("constructor chaining set the id", "PAT-9001", patient.getId());
        assertEquals("constructor chaining set the name", "Test Patient", patient.getName());

        assertTrue("65-year-old is a senior citizen", patient.isSeniorCitizen());
        assertEquals("age group derived correctly", "SENIOR", patient.getAgeGroup());
        assertEquals("contact number masked", "******3210", patient.getMaskedContactNumber());

        assertEquals("doctor seniority band", "CONSULTANT", doctor.getSeniorityBand());
        assertEquals("entity type reported", "Patient", patient.getEntityType());
        assertEquals("entity type reported", "Doctor", doctor.getEntityType());

        // equals / hashCode contract
        Patient same = new Patient("PAT-9001", "Different Name", 20, "9999999999", "none");
        assertTrue("equal ids mean equal entities", patient.equals(same));
        assertEquals("equal objects hash alike", patient.hashCode(), same.hashCode());
        // The next three assertions bind through Object-typed references on
        // purpose. Written directly -- patient.equals(doctor), .equals(null),
        // .equals(patient) -- the compiler and static analysis can prove each
        // result without running anything, and flag them as defects. But proving
        // equals() returns false for an unrelated type IS the contract; the test
        // has to make the call at run time to be worth anything. Going through
        // Object keeps the runtime check honest while stating the intent: we are
        // exercising the equals contract, not comparing two things we expect to
        // match.
        Object unrelatedType = doctor;
        Object nullReference = null;
        Object sameInstance = patient;

        assertFalse("different types are never equal", patient.equals(unrelatedType));
        assertFalse("null is never equal", patient.equals(nullReference));
        assertTrue("reflexive", patient.equals(sameInstance));

        // Searchable default methods
        assertTrue("matches on name", patient.matches("Test"));
        assertTrue("matches case-insensitively", patient.matches("test patient"));
        assertTrue("blank keyword matches everything", patient.matches(""));
        assertFalse("non-matching keyword rejected", patient.matches("zzzz"));
        assertTrue("doctor searchable by speciality", doctor.matches("cardiology"));
        assertTrue("matchesAll with two hits", patient.matchesAll("Test", "PAT"));
        assertFalse("matchesAll fails when one misses", patient.matchesAll("Test", "zzzz"));
        assertTrue("matchesAny succeeds on one hit", patient.matchesAny("zzzz", "Test"));

        // Encapsulation — the returned list must be a read-only view.
        assertThrows("medical history view is unmodifiable", UnsupportedOperationException.class,
                () -> patient.getMedicalHistory().add("injected"));
    }

    private static void runCopySemanticsTests() {
        suite("Cloning — deep vs shallow copy");

        Patient original = new Patient("PAT-9100", "Copy Source", 40, "9876543210", "diabetes");
        original.addAllergy("Penicillin");

        try {
            Patient deep = original.clone();
            Patient shallow = original.shallowCopy();

            assertFalse("deep copy is a different object", original == deep);
            assertTrue("deep copy is still equal by id", original.equals(deep));
            assertEquals("deep copy carries the history", 1, deep.getMedicalHistory().size());

            // The decisive test: mutate the original and observe both copies.
            original.addMedicalHistoryEntry("NEW ENTRY");

            assertEquals("original now has 2 history entries", 2, original.getMedicalHistory().size());
            assertEquals("deep copy is unaffected", 1, deep.getMedicalHistory().size());
            assertEquals("shallow copy shares the list and changed too",
                    2, shallow.getMedicalHistory().size());

            original.addAllergy("Sulfa");
            assertEquals("deep copy allergies unaffected", 1, deep.getAllergies().size());
            assertEquals("shallow copy allergies changed", 2, shallow.getAllergies().size());

        } catch (CloneNotSupportedException e) {
            fail("clone threw CloneNotSupportedException: " + e.getMessage());
        }

        // Appointment: deep-copies its patient, deliberately shares its doctor.
        Doctor doctor = new Doctor("DOC-9100", "Shared Doctor", 50, "9876543211",
                Specialization.NEUROLOGY, 1500, 20, 4.8);
        Appointment appointment = new Appointment("APT-9100", original, doctor,
                LocalDateTime.now().plusDays(1));

        try {
            Appointment copy = appointment.clone();
            assertFalse("appointment copy is a different object", appointment == copy);
            assertFalse("nested patient was deep-copied",
                    appointment.getPatient() == copy.getPatient());
            assertTrue("doctor is intentionally shared by reference",
                    appointment.getDoctor() == copy.getDoctor());

            copy.addSymptom("added to copy only");
            assertEquals("original symptoms untouched", 0, appointment.getSymptoms().size());
            assertEquals("copy has its own symptom list", 1, copy.getSymptoms().size());

        } catch (CloneNotSupportedException e) {
            fail("appointment clone threw: " + e.getMessage());
        }
    }

    private static void runImmutabilityTests() {
        suite("BillSummary — immutability");

        Patient patient = new Patient("PAT-9200", "Immutable Test", 30, "9876543210", "none");
        Bill bill = BillFactory.createBill(BillType.CONSULTATION, patient, "APT-9200", 1000);
        BillSummary summary = bill.generateBill();

        assertNotNull("summary produced", summary);
        assertEquals("patient id captured", "PAT-9200", summary.getPatientId());

        assertThrows("line items are unmodifiable", UnsupportedOperationException.class,
                () -> summary.getLineItems().add(new Bill.LineItem("injected", 1)));

        double totalBefore = summary.getTotalAmount();
        BillSummary withPayment = summary.withPayment(500);
        assertFalse("withPayment returns a new object", summary == withPayment);
        assertEquals("original summary is untouched", 0.0, summary.getAmountPaid());
        assertEquals("new summary records the payment", 500.0, withPayment.getAmountPaid());
        assertEquals("total is unchanged by payment", totalBefore, withPayment.getTotalAmount());

        assertTrue("BillSummary class is final",
                java.lang.reflect.Modifier.isFinal(BillSummary.class.getModifiers()));

        boolean allFieldsFinal = java.util.Arrays.stream(BillSummary.class.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .allMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        assertTrue("every instance field is final", allFieldsFinal);

        boolean hasSetters = java.util.Arrays.stream(BillSummary.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().startsWith("set"));
        assertFalse("no setter methods exist", hasSetters);
    }

    private static void runEnumTests() {
        suite("Enums — state machine and lookups");

        assertTrue("PENDING can become CONFIRMED",
                AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.CONFIRMED));
        assertTrue("PENDING can become CANCELLED",
                AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.CANCELLED));
        assertFalse("PENDING cannot jump straight to COMPLETED",
                AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.COMPLETED));
        assertTrue("CONFIRMED can become COMPLETED",
                AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.COMPLETED));
        assertFalse("CANCELLED is terminal",
                AppointmentStatus.CANCELLED.canTransitionTo(AppointmentStatus.CONFIRMED));
        assertFalse("a status cannot transition to itself",
                AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.PENDING));

        assertTrue("CANCELLED reports terminal", AppointmentStatus.CANCELLED.isTerminal());
        assertFalse("PENDING is not terminal", AppointmentStatus.PENDING.isTerminal());
        assertTrue("only COMPLETED is billable", AppointmentStatus.COMPLETED.isBillable());
        assertFalse("CONFIRMED is not billable", AppointmentStatus.CONFIRMED.isBillable());

        assertEquals("enum lookup by name", AppointmentStatus.CONFIRMED,
                AppointmentStatus.fromString("confirmed").orElse(null));
        assertEquals("enum lookup by display name", AppointmentStatus.NO_SHOW,
                AppointmentStatus.fromString("No Show").orElse(null));
        assertTrue("unknown status returns empty",
                AppointmentStatus.fromString("nonsense").isEmpty());

        assertEquals("speciality lookup", Specialization.CARDIOLOGY,
                Specialization.fromString("cardiology").orElse(null));
        assertEquals("speciality lookup by display name", Specialization.GENERAL_PRACTICE,
                Specialization.fromString("General Practice").orElse(null));
        assertTrue("speciality scores against matching symptoms",
                Specialization.CARDIOLOGY.scoreAgainst("severe chest pain and palpitation") >= 2);
        assertEquals("speciality scores zero on unrelated text", 0,
                Specialization.CARDIOLOGY.scoreAgainst("broken ankle"));
        assertThrows("keyword list is unmodifiable", UnsupportedOperationException.class,
                () -> Specialization.CARDIOLOGY.getSymptomKeywords().add("x"));
    }

    private static void runSingletonTests() {
        suite("Singletons — eager and lazy");

        assertTrue("AppConfig returns one instance (eager)",
                AppConfig.getInstance() == AppConfig.getInstance());
        assertTrue("IdGenerator returns one instance (lazy holder)",
                IdGenerator.getInstance() == IdGenerator.getInstance());

        assertNotNull("config exposes app name", AppConfig.getInstance().get("app.name"));
        assertEquals("config default honoured", "fallback",
                AppConfig.getInstance().get("missing.key", "fallback"));
        assertThrows("settings map is unmodifiable", UnsupportedOperationException.class,
                () -> AppConfig.getInstance().getAllSettings().put("k", "v"));

        IdGenerator generator = IdGenerator.getInstance();
        generator.resetAll();

        String first = generator.nextPatientId();
        String second = generator.nextPatientId();
        assertEquals("first id is zero-padded", "PAT-0001", first);
        assertEquals("ids increment", "PAT-0002", second);
        assertFalse("ids are unique", first.equals(second));

        assertEquals("doctor prefix honoured", "DOC-0001", generator.nextDoctorId());
        assertEquals("appointment prefix honoured", "APT-0001", generator.nextAppointmentId());
        assertEquals("bill prefix honoured", "BIL-0001", generator.nextBillId());

        generator.syncFromId("PAT-0099");
        assertEquals("counter fast-forwarded after import", "PAT-0100", generator.nextPatientId());
        generator.syncFromId("PAT-0050");
        assertEquals("sync never moves a counter backwards", "PAT-0101", generator.nextPatientId());
        assertNoThrow("malformed id is ignored by sync", () -> {
            generator.syncFromId("GARBAGE");
            return "ok";
        });

        generator.resetAll();
    }

    private static void runDataStoreTests() {
        suite("DataStore<T> — generic CRUD");

        DataStore<Patient> store = new DataStore<>("Patient");
        assertTrue("new store is empty", store.isEmpty());
        assertEquals("empty store counts zero", 0, store.count());

        Patient one = new Patient("PAT-8001", "Alpha", 30, "9876543210", "none");
        Patient two = new Patient("PAT-8002", "Beta", 65, "9876543211", "diabetes");
        store.save(one);
        store.save(two);

        assertEquals("two entities stored", 2, store.count());
        assertTrue("exists finds a stored id", store.exists("PAT-8001"));
        assertFalse("exists rejects an unknown id", store.exists("PAT-9999"));
        assertTrue("findById returns a value", store.findById("PAT-8001").isPresent());
        assertTrue("findById on a miss returns empty", store.findById("nope").isEmpty());
        assertTrue("findById(null) returns empty", store.findById(null).isEmpty());

        assertNoThrow("getById returns the entity", () -> store.getById("PAT-8001"));
        assertThrows("getById throws on a miss", EntityNotFoundException.class,
                () -> store.getById("PAT-9999"));

        store.save(new Patient("PAT-8001", "Alpha Updated", 31, "9876543210", "none"));
        assertEquals("save on an existing id replaces rather than duplicates", 2, store.count());
        assertEquals("replacement took effect", "Alpha Updated",
                store.findById("PAT-8001").map(Patient::getName).orElse(null));

        assertEquals("predicate filter works", 1, store.findBy(Patient::isSeniorCitizen).size());
        assertEquals("keyword search works", 1, store.search("Beta").size());
        assertEquals("blank keyword returns everything", 2, store.search("").size());
        assertEquals("countBy matches findBy", 1L, store.countBy(Patient::isSeniorCitizen));

        assertTrue("deleteById removes", store.deleteById("PAT-8001"));
        assertFalse("deleteById on a miss returns false", store.deleteById("PAT-8001"));
        assertEquals("count reflects the delete", 1, store.count());

        store.clear();
        assertTrue("clear empties the store", store.isEmpty());
    }

    private static void runGenericsAndIteratorTests() {
        suite("Generics and Iterator contract");

        DataStore<Doctor> doctors = new DataStore<>("Doctor");
        doctors.save(new Doctor("DOC-8001", "Iter One", 40, "9876543210",
                Specialization.CARDIOLOGY, 1000, 10, 4.0));
        doctors.save(new Doctor("DOC-8002", "Iter Two", 35, "9876543211",
                Specialization.NEUROLOGY, 1500, 5, 4.5));

        int seen = 0;
        for (Doctor doctor : doctors) {   // works because DataStore implements Iterable
            assertNotNull("iterated doctor is not null", doctor);
            seen++;
        }
        assertEquals("for-each visited every entity", 2, seen);

        Iterator<Doctor> iterator = doctors.iterator();
        assertTrue("iterator has a first element", iterator.hasNext());
        iterator.next();
        iterator.next();
        assertFalse("iterator is exhausted", iterator.hasNext());
        assertThrows("next past the end throws", java.util.NoSuchElementException.class,
                iterator::next);

        assertThrows("iterator.remove is unsupported", UnsupportedOperationException.class,
                () -> doctors.iterator().remove());

        // Comparators
        List<Doctor> byFee = doctors.findAllSorted(Doctor.BY_FEE);
        assertEquals("BY_FEE puts the cheapest first", "DOC-8001", byFee.get(0).getId());
        List<Doctor> byRating = doctors.findAllSorted(Doctor.BY_RATING);
        assertEquals("BY_RATING puts the best first", "DOC-8002", byRating.get(0).getId());
        List<Doctor> natural = doctors.findAllSorted(null);
        assertEquals("natural order is by id", "DOC-8001", natural.get(0).getId());
    }

    private static void runPatientServiceTests() {
        suite("PatientService — CRUD and overloaded search");

        PatientService service = new PatientService();
        IdGenerator.getInstance().resetAll();

        try {
            service.addPatient("Ravi Kumar", 67, "9876543210", "O+", false);
            service.addPatient("Meera Joshi", 34, "9876543211", "A+", true);
            service.addPatient("Aditya Verma", 8, "9876543212", "B+", true);
        } catch (InvalidDataException e) {
            fail("seeding patients threw: " + e.getMessage());
        }

        assertEquals("three patients registered", 3, service.count());
        assertThrows("invalid patient rejected at the service boundary", InvalidDataException.class,
                () -> service.addPatient("X", 200, "123"));

        // The four overloads — each resolved by argument type at compile time.
        assertEquals("searchPatient(String) — keyword", 1, service.searchPatient("Ravi").size());
        assertEquals("searchPatient(int) — exact age", 1, service.searchPatient(34).size());
        assertEquals("searchPatient(int,int) — age range", 2, service.searchPatient(30, 70).size());
        assertEquals("searchPatient(String,boolean) — exact name", 1,
                service.searchPatient("Meera Joshi", true).size());
        assertEquals("searchPatient(String,boolean) — partial name", 1,
                service.searchPatient("meera", false).size());
        assertEquals("exact match rejects a partial name", 0,
                service.searchPatient("Meera", true).size());

        assertEquals("senior citizens identified", 1, service.getSeniorCitizens().size());
        assertEquals("insured patients identified", 2, service.getInsuredPatients().size());
        assertTrue("average age computed", service.getAverageAge() > 0);

        assertNoThrow("update applies", () -> service.updatePatient("PAT-0001", "Ravi K", null, null));
        assertThrows("update on a missing id throws", EntityNotFoundException.class,
                () -> service.updatePatient("PAT-9999", "X", null, null));

        assertTrue("delete succeeds", service.deletePatient("PAT-0003"));
        assertEquals("count reflects the delete", 2, service.count());
    }

    private static void runDoctorServiceTests() {
        suite("DoctorService — roster and analytics");

        DoctorService service = new DoctorService();
        IdGenerator.getInstance().resetAll();

        try {
            service.addDoctor("Anita Rao", 44, "9876543210", Specialization.CARDIOLOGY, 1400, 16);
            service.addDoctor("Vikram Nair", 38, "9876543211", Specialization.CARDIOLOGY, 1600, 11);
            service.addDoctor("Priya Sharma", 33, "9876543212", Specialization.DERMATOLOGY, 800, 6);
        } catch (InvalidDataException e) {
            fail("seeding doctors threw: " + e.getMessage());
        }

        assertEquals("three doctors on the roster", 3, service.count());
        assertEquals("search by speciality", 2,
                service.searchDoctor(Specialization.CARDIOLOGY).size());
        assertEquals("search by fee ceiling", 1, service.searchDoctor(900.0).size());
        assertEquals("search by speciality and experience", 1,
                service.searchDoctor(Specialization.CARDIOLOGY, 15).size());
        assertEquals("keyword search", 1, service.searchDoctor("Priya").size());

        assertEquals("average cardiology fee", 1500.0,
                service.getAverageFeeBySpecialization().get(Specialization.CARDIOLOGY).doubleValue());
        assertEquals("cardiology headcount", 2.0,
                service.countBySpecialization().get(Specialization.CARDIOLOGY).doubleValue());
        assertEquals("overall average fee", 1266.67,
                Math.round(service.getOverallAverageFee() * 100) / 100.0);
        assertEquals("most expensive doctor found", "DOC-0002",
                service.getMostExpensiveDoctor().map(Doctor::getId).orElse(null));

        assertNoThrow("rating recorded", () -> {
            service.rateDoctor("DOC-0001", 4.5);
            return "ok";
        });
        assertTrue("unstaffed specialities reported",
                service.getUnstaffedSpecializations().contains(Specialization.NEUROLOGY));

        var stats = service.getFeeStatistics();
        assertEquals("fee stats count", 3L, stats.getCount());
        assertEquals("fee stats min", 800.0, stats.getMin());
        assertEquals("fee stats max", 1600.0, stats.getMax());
    }

    private static void runAppointmentServiceTests() {
        suite("AppointmentService — booking rules and lifecycle");

        AppointmentService service = new AppointmentService();
        IdGenerator.getInstance().resetAll();

        Patient patient = new Patient("PAT-7001", "Book Test", 40, "9876543210", "none");
        Doctor doctor = new Doctor("DOC-7001", "Slot Doctor", 45, "9876543211",
                Specialization.GENERAL_PRACTICE, 500, 10, 4.0);

        LocalDateTime slot = DateUtil.alignToSlot(
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        Appointment appointment = assertPresent("appointment booked",
                tryGet(() -> service.bookAppointment(patient, doctor, slot,
                        List.of("fever", "cough"))));
        assertEquals("new appointment starts PENDING",
                AppointmentStatus.PENDING, appointment.getStatus());
        assertEquals("symptoms recorded", 2, appointment.getSymptoms().size());

        assertThrows("double-booking the same slot is refused", SlotUnavailableException.class,
                () -> service.bookAppointment(patient, doctor, slot));

        assertThrows("past slot refused", InvalidDataException.class,
                () -> service.bookAppointment(patient, doctor, LocalDateTime.now().minusDays(1)));
        assertThrows("slot outside clinic hours refused", InvalidDataException.class,
                () -> service.bookAppointment(patient, doctor,
                        LocalDateTime.now().plusDays(1).withHour(3).withMinute(0)));
        assertThrows("null patient refused", InvalidDataException.class,
                () -> service.bookAppointment(null, doctor, slot.plusHours(2)));

        // Checked BEFORE the lifecycle transitions below: once an appointment reaches a
        // terminal status its slot is released, so this must be asserted while it is live.
        assertFalse("a live booking removes its slot from availability",
                service.getAvailableSlots("DOC-7001", slot.toLocalDate()).contains(slot));

        // Lifecycle, driven through the enum's state machine.
        String id = appointment.getId();
        assertThrows("cannot complete a PENDING appointment", InvalidDataException.class,
                () -> service.completeAppointment(id));
        assertNoThrow("confirm succeeds", () -> service.confirmAppointment(id));
        assertEquals("status is CONFIRMED", AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertNoThrow("complete succeeds after confirm", () -> service.completeAppointment(id));
        assertEquals("status is COMPLETED", AppointmentStatus.COMPLETED, appointment.getStatus());
        assertThrows("cannot cancel a COMPLETED appointment", InvalidDataException.class,
                () -> service.cancelAppointment(id));

        assertThrows("unknown appointment id throws", AppointmentNotFoundException.class,
                () -> service.confirmAppointment("APT-9999"));

        assertEquals("appointments for patient", 1,
                service.getAppointmentsForPatient("PAT-7001").size());
        assertEquals("appointments for doctor", 1,
                service.getAppointmentsForDoctor("DOC-7001").size());
        assertTrue("a completed appointment releases its slot back to availability",
                service.getAvailableSlots("DOC-7001", slot.toLocalDate()).contains(slot));
    }

    private static void runBillingAndFactoryTests() {
        suite("Billing — Factory and Template Method");

        Patient standard = new Patient("PAT-6001", "Standard Payer", 35, "9876543210", "seen before");
        Patient senior = new Patient("PAT-6002", "Senior Payer", 70, "9876543211", "seen before");
        Patient insured = new Patient("PAT-6003", "Insured Payer", 40, "9876543212", "seen before");
        insured.setInsured(true);

        // Factory returns the right concrete type for each discriminator.
        Bill consultation = BillFactory.createBill(BillType.CONSULTATION, standard, null, 1000);
        Bill procedure = BillFactory.createBill(BillType.PROCEDURE, standard, null, 1000);
        Bill emergency = BillFactory.createBill(BillType.EMERGENCY, standard, null, 1000);

        assertTrue("factory built a ConsultationBill",
                consultation instanceof com.airtribe.meditrack.entity.ConsultationBill);
        assertTrue("factory built a ProcedureBill",
                procedure instanceof com.airtribe.meditrack.entity.ProcedureBill);
        assertTrue("factory built an EmergencyBill",
                emergency instanceof com.airtribe.meditrack.entity.EmergencyBill);
        assertTrue("factory defaults a null type to CONSULTATION",
                BillFactory.createBill(null, standard, null, 100)
                        instanceof com.airtribe.meditrack.entity.ConsultationBill);

        // Template Method: same call, different results, via overridden steps.
        consultation.generateBill();
        emergency.generateBill();
        assertEquals("no surcharge on a consultation", 0.0, consultation.getSurchargeAmount());
        assertTrue("emergency applies a surcharge", emergency.getSurchargeAmount() > 0);
        assertTrue("emergency total exceeds consultation total",
                emergency.getTotalAmount() > consultation.getTotalAmount());

        assertEquals("bill descriptions are polymorphic",
                "Consultation Bill", consultation.getBillDescription());
        assertEquals("bill descriptions are polymorphic",
                "Emergency Bill", emergency.getBillDescription());

        // Tax is applied on top of base + surcharge.
        double expectedTax = (consultation.getBaseAmount() + consultation.getSurchargeAmount())
                * Constants.TAX_RATE;
        assertEquals("GST computed at the configured rate",
                Math.round(expectedTax * 100) / 100.0,
                Math.round(consultation.getTaxAmount() * 100) / 100.0);

        // Factory picks a strategy from patient attributes.
        assertTrue("insured patient gets the insurance strategy",
                BillFactory.chooseStrategy(insured) instanceof InsuranceBillingStrategy);
        assertTrue("senior patient gets the concession strategy",
                BillFactory.chooseStrategy(senior) instanceof SeniorCitizenBillingStrategy);
        assertTrue("ordinary patient gets the standard strategy",
                BillFactory.chooseStrategy(standard) instanceof StandardBillingStrategy);
        assertTrue("null patient falls back to standard",
                BillFactory.chooseStrategy(null) instanceof StandardBillingStrategy);

        // Payable contract
        BillingService billing = new BillingService();
        Bill bill = assertPresent("bill raised through the service",
                tryGet(() -> billing.generateBill(standard, BillType.CONSULTATION, 1000)));
        assertEquals("nothing paid yet", "UNPAID", bill.getPaymentStatus());
        assertFalse("not fully paid", bill.isFullyPaid());

        double due = bill.getAmountDue();
        assertTrue("partial payment accepted", bill.processPayment(due / 2));
        assertEquals("status is partial", "PARTIALLY_PAID", bill.getPaymentStatus());
        assertFalse("overpayment refused", bill.processPayment(due));
        assertFalse("zero payment refused", bill.processPayment(0));
        assertFalse("negative payment refused", bill.processPayment(-100));
        assertTrue("settling the remainder is accepted", bill.processPayment(bill.getAmountDue()));
        assertEquals("status is paid", "PAID", bill.getPaymentStatus());
        assertTrue("fully paid", bill.isFullyPaid());

        assertNotNull("formatted bill rendered", bill.getFormattedBill());
        assertTrue("formatted bill names the patient",
                bill.getFormattedBill().contains("Standard Payer"));
        assertEquals("currency formatting", "₹1,000.00", Payable.formatCurrency(1000));
    }

    private static void runStrategyTests() {
        suite("Strategy — interchangeable pricing policies");

        double base = 1000.0;

        assertEquals("standard leaves the amount alone",
                1000.0, new StandardBillingStrategy().calculate(base));
        assertEquals("insurance leaves the 30% co-pay",
                300.0, Math.round(new InsuranceBillingStrategy().calculate(base) * 100) / 100.0);
        assertEquals("senior concession takes 10% off",
                900.0, new SeniorCitizenBillingStrategy().calculate(base));

        assertEquals("custom coverage honoured", 100.0,
                new InsuranceBillingStrategy(0.90, "MaxCover").calculate(base));
        assertEquals("coverage above 1.0 is clamped", 0.0,
                new InsuranceBillingStrategy(5.0, "Absurd").calculate(base));
        assertEquals("negative coverage is clamped", 1000.0,
                new InsuranceBillingStrategy(-1.0, "Absurd").calculate(base));

        assertNotNull("strategies describe themselves",
                new SeniorCitizenBillingStrategy().describe());
        assertEquals("strategy name reported",
                "Standard", new StandardBillingStrategy().getStrategyName());

        // The interface is functional, so a lambda is a valid policy.
        com.airtribe.meditrack.interfaces.BillingStrategy flatFifty = amount -> amount - 50;
        assertEquals("lambda strategy applies", 950.0, flatFifty.calculate(base));

        // Same bill, different policy, different total — the point of Strategy.
        Patient patient = new Patient("PAT-5001", "Strategy Test", 30, "9876543210", "seen");
        BillingService service = new BillingService();
        Bill standardBill = assertPresent("standard bill raised",
                tryGet(() -> service.generateBillWithStrategy(
                        patient, BillType.CONSULTATION, base, new StandardBillingStrategy())));
        Bill insuredBill = assertPresent("insured bill raised",
                tryGet(() -> service.generateBillWithStrategy(
                        patient, BillType.CONSULTATION, base, new InsuranceBillingStrategy())));

        assertTrue("insurance produces a smaller total",
                insuredBill.getTotalAmount() < standardBill.getTotalAmount());
    }

    private static void runObserverTests() {
        suite("Observer — notification fan-out");

        NotificationService notifications = new NotificationService();
        AuditLogObserver audit = new AuditLogObserver();
        SmsReminderObserver sms = new SmsReminderObserver();

        notifications.register(audit);
        notifications.register(sms);
        assertEquals("two observers registered", 2, notifications.getObserverCount());

        notifications.register(audit);
        assertEquals("duplicate registration ignored", 2, notifications.getObserverCount());
        notifications.register(null);
        assertEquals("null registration ignored", 2, notifications.getObserverCount());

        AppointmentService appointments = new AppointmentService(
                new DataStore<>("Appointment"), notifications);

        Patient patient = new Patient("PAT-4001", "Observer Test", 30, "9876543210", "none");
        Doctor doctor = new Doctor("DOC-4001", "Observer Doctor", 40, "9876543211",
                Specialization.GENERAL_PRACTICE, 500, 5, 4.0);

        Appointment appointment = tryGet(() -> appointments.bookAppointment(patient, doctor,
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0)));

        assertNotNull("appointment booked", appointment);
        assertTrue("audit recorded the booking", audit.size() >= 1);
        assertTrue("SMS was sent for the booking", sms.getMessagesSent() >= 1);

        int auditBefore = audit.size();
        assertNoThrow("cancellation dispatches an event",
                () -> appointments.cancelAppointment(appointment.getId()));
        assertTrue("audit recorded the cancellation", audit.size() > auditBefore);

        // Selective subscription
        assertTrue("SMS wants BOOKED", sms.isInterestedIn(NotificationService.EVENT_BOOKED));
        assertFalse("SMS ignores COMPLETED", sms.isInterestedIn(NotificationService.EVENT_COMPLETED));
        assertTrue("audit wants everything", audit.isInterestedIn("ANYTHING"));

        assertTrue("unregister works", notifications.unregister(sms));
        assertEquals("observer count drops", 1, notifications.getObserverCount());

        notifications.setEnabled(false);
        int countWhenDisabled = notifications.getEventsDispatched();
        notifications.notifyObservers(appointment, NotificationService.EVENT_REMINDER);
        assertEquals("disabled service dispatches nothing",
                countWhenDisabled, notifications.getEventsDispatched());

        assertThrows("audit trail is unmodifiable", UnsupportedOperationException.class,
                () -> audit.getEntries().add(null));
    }

    private static void runAIHelperTests() {
        suite("AIHelper — rule-based triage");

        assertEquals("chest pain routes to cardiology", Specialization.CARDIOLOGY,
                AIHelper.recommendSpecialization("severe chest pain and palpitation"));
        assertEquals("migraine routes to neurology", Specialization.NEUROLOGY,
                AIHelper.recommendSpecialization("throbbing headache and migraine"));
        assertEquals("rash routes to dermatology", Specialization.DERMATOLOGY,
                AIHelper.recommendSpecialization("itching skin rash"));
        assertEquals("unknown symptoms fall back to general practice",
                Specialization.GENERAL_PRACTICE,
                AIHelper.recommendSpecialization("zzzz qqqq unknown complaint"));
        assertEquals("null symptoms fall back to general practice",
                Specialization.GENERAL_PRACTICE, AIHelper.recommendSpecialization(null));

        assertTrue("urgency detected", AIHelper.isUrgent("severe bleeding after an accident"));
        assertFalse("routine complaint is not urgent", AIHelper.isUrgent("mild cold"));
        assertFalse("null is not urgent", AIHelper.isUrgent(null));

        List<AIHelper.SpecialityMatch> matches =
                AIHelper.recommendSpecialities("chest pain and breathless");
        assertFalse("matches returned", matches.isEmpty());
        assertEquals("best match is cardiology",
                Specialization.CARDIOLOGY, matches.get(0).specialization());
        assertTrue("match records the keywords that fired",
                matches.get(0).matchedKeywords().size() >= 2);

        List<Doctor> doctors = List.of(
                new Doctor("DOC-3001", "Heart Expert", 50, "9876543210",
                        Specialization.CARDIOLOGY, 1500, 20, 4.9),
                new Doctor("DOC-3002", "Skin Expert", 35, "9876543211",
                        Specialization.DERMATOLOGY, 800, 5, 4.0));

        List<AIHelper.DoctorRecommendation> recommendations =
                AIHelper.recommendDoctors("chest pain", doctors, List.of(), 2);
        assertEquals("both doctors ranked", 2, recommendations.size());
        assertEquals("the cardiologist ranks first",
                "DOC-3001", recommendations.get(0).doctor().getId());
        assertTrue("the top recommendation scores higher",
                recommendations.get(0).score() > recommendations.get(1).score());
        assertNotNull("recommendation explains itself", recommendations.get(0).reason());

        assertTrue("no doctors yields no recommendations",
                AIHelper.recommendDoctors("chest pain", List.of(), List.of(), 3).isEmpty());

        List<LocalDateTime> slots = AIHelper.suggestSlots(doctors.get(0), List.of(), 3, 7);
        assertEquals("three slots suggested", 3, slots.size());
        assertTrue("suggested slots are in the future", DateUtil.isFuture(slots.get(0)));
        assertTrue("suggested slots are within clinic hours",
                DateUtil.isWithinClinicHours(slots.get(0)));
        assertTrue("null doctor yields no slots",
                AIHelper.suggestSlots(null, List.of(), 3, 7).isEmpty());

        assertNotNull("triage report renders",
                AIHelper.buildTriageReport("chest pain", doctors, List.of()));

        Patient bare = new Patient("PAT-3001", "Bare Record", 70, "9876543210", "");
        assertFalse("screening prompts produced for a sparse record",
                AIHelper.suggestScreeningPrompts(bare).isEmpty());
        assertTrue("null patient yields no prompts",
                AIHelper.suggestScreeningPrompts(null).isEmpty());
    }

    private static void runExceptionTests() {
        suite("Exceptions — custom types and chaining");

        InvalidDataException simple = new InvalidDataException("something went wrong");
        assertEquals("message preserved", "something went wrong", simple.getMessage());
        assertEquals("no field on the simple form", null, simple.getFieldName());

        InvalidDataException detailed = new InvalidDataException("age", 200, "out of range");
        assertEquals("field captured", "age", detailed.getFieldName());
        assertEquals("rejected value captured", 200, detailed.getRejectedValue());
        assertTrue("message names the field", detailed.getMessage().contains("age"));

        RuntimeException root = new RuntimeException("disk on fire");
        InvalidDataException chained = new InvalidDataException("f", "v", "bad", root);
        assertTrue("cause preserved", chained.getCause() == root);

        AppointmentNotFoundException notFound = new AppointmentNotFoundException("APT-0404");
        assertEquals("id captured", "APT-0404", notFound.getAppointmentId());
        assertTrue("message names the id", notFound.getMessage().contains("APT-0404"));

        EntityNotFoundException entityMissing = new EntityNotFoundException("Patient", "PAT-0404");
        assertEquals("entity type captured", "Patient", entityMissing.getEntityType());
        assertTrue("message names both", entityMissing.getMessage().contains("PAT-0404"));

        SlotUnavailableException slotTaken = new SlotUnavailableException(
                "DOC-0001", LocalDateTime.now(), "already booked");
        assertEquals("doctor id captured", "DOC-0001", slotTaken.getDoctorId());

        DataPersistenceException ioFailure = new DataPersistenceException(
                "data/x.csv", "write failed", root);
        assertEquals("file path captured", "data/x.csv", ioFailure.getFilePath());
        assertTrue("cause preserved through the persistence layer", ioFailure.getCause() == root);

        assertTrue("custom exceptions are checked", Exception.class.isAssignableFrom(
                InvalidDataException.class) && !RuntimeException.class.isAssignableFrom(
                InvalidDataException.class));
    }

    private static void runCSVTests() {
        suite("CSV — round-trip and try-with-resources");

        assertEquals("comma escaped", "a||b", CSVUtil.escape("a,b"));
        assertEquals("escape reversed", "a,b", CSVUtil.unescape("a||b"));
        assertEquals("null escapes to empty", "", CSVUtil.escape(null));
        assertEquals("blank unescapes to null", null, CSVUtil.unescape("  "));
        assertEquals("newlines flattened", "a b", CSVUtil.escape("a\nb"));

        assertEquals("list joined", "x;y;z", CSVUtil.joinList(List.of("x", "y", "z")));
        assertEquals("list split", 3, CSVUtil.splitList("x;y;z").size());
        assertTrue("empty cell splits to an empty list", CSVUtil.splitList("").isEmpty());

        Patient patient = new Patient("PAT-2001", "CSV Test", 45, "9876543210",
                List.of("condition, with comma", "second"), List.of("Penicillin"), "O+", true);

        String row = CSVUtil.toCsvRow(patient);
        Patient restored = CSVUtil.patientFromCsvRow(row);

        assertNotNull("patient round-tripped", restored);
        assertEquals("id survived", patient.getId(), restored.getId());
        assertEquals("name survived", patient.getName(), restored.getName());
        assertEquals("age survived", patient.getAge(), restored.getAge());
        assertEquals("insured flag survived", patient.isInsured(), restored.isInsured());
        assertEquals("blood group survived", patient.getBloodGroup(), restored.getBloodGroup());
        assertEquals("history count survived", 2, restored.getMedicalHistory().size());
        assertTrue("comma inside a field survived escaping",
                restored.getMedicalHistory().get(0).contains(","));

        Doctor doctor = new Doctor("DOC-2001", "CSV Doctor", 40, "9876543211",
                Specialization.NEUROLOGY, 1500, 12, 4.6);
        Doctor restoredDoctor = CSVUtil.doctorFromCsvRow(CSVUtil.toCsvRow(doctor));
        assertNotNull("doctor round-tripped", restoredDoctor);
        assertEquals("speciality survived",
                Specialization.NEUROLOGY, restoredDoctor.getSpecialization());
        assertEquals("fee survived", 1500.0, restoredDoctor.getConsultationFee());

        assertEquals("malformed row returns null", null, CSVUtil.patientFromCsvRow("too,few"));
        assertEquals("non-numeric age returns null", null,
                CSVUtil.patientFromCsvRow("PAT-1,Name,NOTANUMBER,9876543210,O+,false,,"));

        // Real file I/O through try-with-resources.
        String testFile = "data/test-patients.csv";
        try {
            CSVUtil.writePatients(testFile, List.of(patient));
            assertTrue("file written", Files.exists(Paths.get(testFile)));

            List<Patient> readBack = CSVUtil.readPatients(testFile);
            assertEquals("one patient read back", 1, readBack.size());
            assertEquals("read-back id matches", "PAT-2001", readBack.get(0).getId());

            Files.deleteIfExists(Paths.get(testFile));

        } catch (DataPersistenceException | java.io.IOException e) {
            fail("CSV file round-trip threw: " + e.getMessage());
        }

        assertNoThrow("reading a missing file returns empty, not an exception",
                () -> CSVUtil.readPatients("data/definitely-not-here.csv"));
    }

    private static void runConcurrencyTests() {
        suite("Concurrency — AtomicInteger and thread safety");

        IdGenerator generator = IdGenerator.getInstance();
        generator.resetAll();

        final int threads = 10;
        final int idsPerThread = 100;
        java.util.Set<String> ids = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

        // try-with-resources: ExecutorService is AutoCloseable as of Java 19, and
        // close() shuts the pool down and waits for termination. The old
        // shutdown()-in-finally leaked the pool's threads if await() threw before
        // reaching it.
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        for (int j = 0; j < idsPerThread; j++) {
                            ids.add(generator.nextPatientId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = latch.await(10, TimeUnit.SECONDS);
            assertTrue("all threads finished", finished);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("interrupted while awaiting threads");
        }

        // The decisive assertion: with a plain int++ this would be short.
        // The cast forces double multiplication; int*int silently truncates before
        // widening, which would hide an overflow if the counts were ever raised.
        assertEquals("AtomicInteger issued " + (threads * idsPerThread) + " unique ids with no collisions",
                (double) threads * idsPerThread, ids.size());

        generator.resetAll();

        // Observer dispatch from a background thread must not corrupt the counter.
        NotificationService notifications = new NotificationService();
        AuditLogObserver audit = new AuditLogObserver();
        notifications.register(audit);

        Patient patient = new Patient("PAT-1001", "Thread Test", 30, "9876543210", "none");
        Doctor doctor = new Doctor("DOC-1001", "Thread Doctor", 40, "9876543211",
                Specialization.GENERAL_PRACTICE, 500, 5, 4.0);
        Appointment appointment = new Appointment("APT-1001", patient, doctor,
                LocalDateTime.now().plusHours(2));

        int sent = notifications.sweepOnce(List.of(appointment));
        assertEquals("appointment inside the reminder window was reminded", 1, sent);
        assertTrue("audit captured the reminder", audit.size() >= 1);

        Appointment distant = new Appointment("APT-1002", patient, doctor,
                LocalDateTime.now().plusDays(30));
        assertEquals("appointment outside the window is skipped", 0,
                notifications.sweepOnce(List.of(distant)));

        assertNoThrow("scheduler starts and stops cleanly", () -> {
            notifications.startReminderScheduler(List::of, 3600);
            boolean running = notifications.isSchedulerRunning();
            notifications.stopReminderScheduler();
            return running && !notifications.isSchedulerRunning() ? "ok" : null;
        });

        assertTrue("entity creation counter is populated",
                MedicalEntity.getTotalEntitiesCreated() > 0);
        assertNotNull("static block recorded the class-load time",
                MedicalEntity.getClassLoadedAt());
    }

    private static void runStreamsTests() {
        suite("Streams and lambdas — analytics");

        DoctorService doctors = new DoctorService();
        PatientService patients = new PatientService();
        IdGenerator.getInstance().resetAll();

        try {
            doctors.addDoctor("Stream One", 40, "9876543210", Specialization.CARDIOLOGY, 1000, 10);
            doctors.addDoctor("Stream Two", 45, "9876543211", Specialization.CARDIOLOGY, 2000, 15);
            doctors.addDoctor("Stream Three", 35, "9876543212", Specialization.DERMATOLOGY, 500, 5);

            patients.addPatient("Young Patient", 25, "9876543213", "O+", false);
            patients.addPatient("Senior Patient", 70, "9876543214", "A+", true);
            patients.addPatient("Child Patient", 10, "9876543215", "B+", true);
        } catch (InvalidDataException e) {
            fail("seeding for stream tests threw: " + e.getMessage());
        }

        assertEquals("groupingBy + averagingDouble", 1500.0,
                doctors.getAverageFeeBySpecialization().get(Specialization.CARDIOLOGY).doubleValue());
        assertEquals("groupingBy + counting", 2.0,
                doctors.countBySpecialization().get(Specialization.CARDIOLOGY).doubleValue());
        assertEquals("limit after sorted", 2, doctors.getTopRatedDoctors(2).size());
        assertEquals("summaryStatistics in one pass", 3L,
                doctors.getFeeStatistics().getCount());

        assertEquals("filter + count on patients", 1, patients.getSeniorCitizens().size());
        assertEquals("boolean filter", 2, patients.getInsuredPatients().size());
        assertEquals("mapToInt + average", 35.0, patients.getAverageAge());
        assertEquals("groupingBy age band", 3, patients.countByAgeGroup().size());

        // flatMap over nested collections
        try {
            patients.addAllergy("PAT-0001", "Dust");
            patients.addAllergy("PAT-0002", "Dust");
            patients.addAllergy("PAT-0002", "Pollen");
        } catch (Exception e) {
            fail("recording allergies threw: " + e.getMessage());
        }
        List<String> allergies = patients.getAllKnownAllergies();
        assertEquals("flatMap + distinct de-duplicated", 2, allergies.size());
        assertEquals("results sorted", "Dust", allergies.get(0));

        assertTrue("empty store averages to zero without throwing",
                new DoctorService().getOverallAverageFee() == 0.0);
        assertTrue("max on an empty store returns empty",
                new DoctorService().getMostExpensiveDoctor().isEmpty());
    }

    // ================================================================ framework

    private static void suite(String name) {
        currentSuite = name;
        System.out.printf("%n  %s%n  %s%n", name, "-".repeat(Math.max(20, name.length())));
    }

    private static void assertTrue(String description, boolean condition) {
        record(description, condition, "expected true but was false");
    }

    private static void assertFalse(String description, boolean condition) {
        record(description, !condition, "expected false but was true");
    }

    /**
     * Runs one suite, turning anything that escapes it into a recorded failure.
     *
     * <p>Suites used to be called directly, so an unexpected throw propagated out
     * of {@code main} and abandoned every suite after it — one stray
     * {@code NullPointerException} could hide dozens of genuine results. Catching
     * {@link Throwable} is deliberate rather than lazy: {@link #assertPresent}
     * signals with {@link AssertionError}, which is an {@code Error}, not an
     * {@code Exception}.
     *
     * @param name suite label, used if the suite aborts before naming itself
     * @param body the suite to run
     */
    private static void guard(String name, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            currentSuite = name;
            record(name + " suite aborted", false,
                    "threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /**
     * Asserts non-null and hands the value back, so the caller can keep using it.
     *
     * <p>{@link #assertNotNull} records the failure but lets execution continue,
     * so the very next dereference throws {@code NullPointerException} and the
     * run dies pointing at the symptom instead of the cause. Throwing here stops
     * at the real failure, and {@link #guard} turns it into one clean result.
     *
     * @param description what was expected to be present
     * @param value       the possibly-null value
     * @param <T>         value type
     * @return {@code value}, guaranteed non-null
     */
    private static <T> T assertPresent(String description, T value) {
        assertNotNull(description, value);
        if (value == null) {
            throw new AssertionError(description + " — returned null");
        }
        return value;
    }

    private static void assertNotNull(String description, Object value) {
        record(description, value != null, "expected non-null but was null");
    }

    private static void assertEquals(String description, Object expected, Object actual) {
        boolean equal = expected == null ? actual == null : expected.equals(actual);
        record(description, equal, String.format("expected <%s> but was <%s>", expected, actual));
    }

    private static void assertEquals(String description, double expected, double actual) {
        boolean equal = Math.abs(expected - actual) < 0.0001;
        record(description, equal, String.format("expected <%s> but was <%s>", expected, actual));
    }

    /** Asserts that a call throws the expected exception type. */
    private static void assertThrows(String description,
                                     Class<? extends Throwable> expected,
                                     ThrowingRunnable action) {
        try {
            action.run();
            record(description, false, "expected " + expected.getSimpleName() + " but nothing was thrown");
        } catch (Throwable actual) {
            record(description, expected.isInstance(actual),
                    "expected " + expected.getSimpleName()
                            + " but got " + actual.getClass().getSimpleName()
                            + ": " + actual.getMessage());
        }
    }

    /** Asserts that a call completes without throwing and returns non-null. */
    private static void assertNoThrow(String description, ThrowingSupplier<?> action) {
        try {
            Object result = action.get();
            record(description, result != null, "completed but returned null");
        } catch (Throwable t) {
            record(description, false,
                    "threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /** Runs a supplier, converting any checked exception into a test failure. */
    private static <T> T tryGet(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            fail("unexpected " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
    }

    private static void fail(String reason) {
        record("explicit failure", false, reason);
    }

    private static void record(String description, boolean success, String failureDetail) {
        if (success) {
            passed++;
            System.out.printf("    [PASS] %s%n", description);
        } else {
            failed++;
            String entry = String.format("%s -> %s (%s)", currentSuite, description, failureDetail);
            FAILURES.add(entry);
            System.out.printf("    [FAIL] %s%n           %s%n", description, failureDetail);
        }
    }

    private static void printHeader() {
        System.out.println("""

                  ============================================================
                    MediTrack — Manual Test Runner (no JUnit)
                  ============================================================""");
    }

    private static void printSummary() {
        int total = passed + failed;
        double rate = total == 0 ? 0 : (passed * 100.0) / total;

        StringBuilder sb = new StringBuilder(400);
        sb.append('\n').append("  ").append("=".repeat(60)).append('\n')
                .append("    TEST SUMMARY\n")
                .append("  ").append("=".repeat(60)).append('\n')
                .append(String.format("    Total  : %d%n", total))
                .append(String.format("    Passed : %d%n", passed))
                .append(String.format("    Failed : %d%n", failed))
                .append(String.format("    Rate   : %.1f%%%n", rate));

        if (!FAILURES.isEmpty()) {
            sb.append("  ").append("-".repeat(60)).append('\n')
                    .append("    FAILURES\n");
            for (String failure : FAILURES) {
                sb.append("      - ").append(failure).append('\n');
            }
        }

        sb.append("  ").append("=".repeat(60)).append('\n')
                .append(failed == 0 ? "    ALL TESTS PASSED\n" : "    THERE WERE FAILURES\n")
                .append("  ").append("=".repeat(60));

        System.out.println(sb);
    }

    /** A {@link Runnable} that is allowed to throw checked exceptions. */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** A {@link java.util.function.Supplier} that is allowed to throw checked exceptions. */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
