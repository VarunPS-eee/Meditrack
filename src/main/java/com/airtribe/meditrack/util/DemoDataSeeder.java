package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Populates the application with a production-shaped demo dataset.
 *
 * <p>Exists so a reviewer running {@code --seedDemo} sees a clinic mid-operation
 * rather than a set of empty menus — 20 doctors across 14 specialities, 72
 * patients each carrying a real medical history, ~90 appointments spread over
 * every lifecycle state, and bills in every payment state.</p>
 *
 * <p><b>Every patient has a history and at least one allergy record.</b> That is
 * deliberate: a demo where half the records are blank teaches a reviewer
 * nothing about what the screens look like when they are full.</p>
 *
 * <p><b>The dataset is deterministic.</b> {@link Random} is seeded with a fixed
 * constant, so two runs produce byte-identical output. A demo that shuffles
 * between runs cannot be documented, screenshotted or asserted against.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class DemoDataSeeder {

    /** Fixed seed — reproducibility matters more than variety here. */
    private static final long RANDOM_SEED = 20260725L;

    /**
     * Placeholder for a patient with no known allergies.
     *
     * <p>Recorded explicitly rather than left empty. "No known allergies" and
     * "nobody asked" are clinically different, and a blank field cannot tell you
     * which one you are looking at.
     */
    private static final String NO_ALLERGIES = "None recorded";

    private DemoDataSeeder() {
        throw new AssertionError("DemoDataSeeder is a utility class and must not be instantiated.");
    }

    // ------------------------------------------------------------------ records

    /** One doctor to seed. */
    private record DoctorSeed(String name, int age, Specialization specialization,
                              double fee, int years, double rating) { }

    /** One patient to seed, with the history and allergies that make the record real. */
    private record PatientSeed(String name, int age, String bloodGroup, boolean insured,
                               List<String> history, List<String> allergies,
                               Specialization typicalNeed) { }

    // ------------------------------------------------------------------ entry point

    /**
     * Seeds doctors, patients, appointments and bills.
     *
     * @param patientService     patient store to populate
     * @param doctorService      doctor store to populate
     * @param appointmentService appointment store to populate
     * @param billingService     billing store to populate
     */
    public static void seed(PatientService patientService,
                            DoctorService doctorService,
                            AppointmentService appointmentService,
                            BillingService billingService) {
        try {
            List<Doctor> doctors = seedDoctors(doctorService);

            List<PatientSeed> seeds = new ArrayList<>(featuredPatients());
            seeds.addAll(generatedPatients());

            List<Patient> patients = seedPatients(patientService, seeds);
            List<Appointment> appointments =
                    seedAppointments(appointmentService, doctors, patients, seeds);
            int bills = seedBills(billingService, appointments);

            System.out.printf(
                    "  Demo clinic ready — %d doctors across %d specialities, %d patients, "
                            + "%d appointments, %d bills.%n",
                    doctorService.count(),
                    doctorService.countBySpecialization().size(),
                    patientService.count(),
                    appointmentService.count(),
                    bills);
            System.out.println("  Every patient carries a medical history. Try: Menu 1 -> 2, "
                    + "then Menu 5 -> 1 to search.");
        } catch (InvalidDataException e) {
            System.out.println("  [WARN] Demo seeding failed validation: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ doctors

    /**
     * Twenty doctors across fourteen specialities.
     *
     * <p>The four project members appear here under their full formal names. They
     * also appear in the patient roster under an initials-first rendering, so the
     * same person is recognisable in both lists without the two records ever
     * being confused for each other.</p>
     */
    private static List<Doctor> seedDoctors(DoctorService service) throws InvalidDataException {
        List<DoctorSeed> seeds = List.of(
                // The project team — formal format.
                new DoctorSeed("Sunil Kumar B A", 41, Specialization.ENDOCRINOLOGY, 1400, 15, 4.9),
                new DoctorSeed("Varun P S", 39, Specialization.CARDIOLOGY, 1500, 13, 4.8),
                new DoctorSeed("Zubair Ahmed", 37, Specialization.PULMONOLOGY, 1300, 11, 4.7),
                new DoctorSeed("Anushtha Sharma", 34, Specialization.PEDIATRICS, 800, 8, 4.9),

                new DoctorSeed("Anita Rao", 44, Specialization.CARDIOLOGY, 1450, 16, 4.8),
                new DoctorSeed("Vikram Nair", 38, Specialization.NEUROLOGY, 1600, 11, 4.5),
                new DoctorSeed("Priya Sharma", 33, Specialization.DERMATOLOGY, 850, 6, 4.6),
                new DoctorSeed("Rahul Mehta", 51, Specialization.ORTHOPEDICS, 1100, 22, 4.2),
                new DoctorSeed("Sneha Iyer", 36, Specialization.PEDIATRICS, 750, 9, 4.7),
                new DoctorSeed("Arjun Desai", 29, Specialization.GENERAL_PRACTICE, 500, 3, 4.0),
                new DoctorSeed("Kavita Menon", 47, Specialization.GYNECOLOGY, 1200, 19, 4.8),
                new DoctorSeed("Imran Qureshi", 42, Specialization.ENT, 780, 14, 4.4),
                new DoctorSeed("Deepa Krishnan", 45, Specialization.OPHTHALMOLOGY, 950, 17, 4.6),
                new DoctorSeed("Sanjay Gupta", 53, Specialization.PSYCHIATRY, 1350, 24, 4.5),
                new DoctorSeed("Neha Bansal", 31, Specialization.DENTISTRY, 620, 5, 4.3),
                new DoctorSeed("Ramesh Pillai", 49, Specialization.GASTROENTEROLOGY, 1200, 20, 4.6),
                new DoctorSeed("Farah Khan", 40, Specialization.PULMONOLOGY, 1280, 14, 4.7),
                new DoctorSeed("Aditya Chauhan", 35, Specialization.ORTHOPEDICS, 1050, 9, 4.1),
                new DoctorSeed("Lakshmi Narayan", 58, Specialization.NEUROLOGY, 1700, 30, 4.9),
                new DoctorSeed("Rohan Kulkarni", 27, Specialization.GENERAL_PRACTICE, 480, 2, 3.9));

        List<Doctor> created = new ArrayList<>();
        long contact = 9820000001L;
        for (DoctorSeed s : seeds) {
            Doctor d = service.addDoctor(s.name(), s.age(), String.valueOf(contact++),
                    s.specialization(), s.fee(), s.years());
            d.setRating(s.rating());
            created.add(d);
        }
        return created;
    }

    // ------------------------------------------------------------------ patients

    /**
     * Seventy-two patients, every one with a medical history.
     *
     * <p>Twenty-four are hand-written with specific, clinically coherent
     * histories — those are the records worth opening during a walkthrough. The
     * remaining forty-eight are composed from name pools and condition templates,
     * which keeps the file readable while still guaranteeing that
     * <em>every</em> patient id returns a populated history.</p>
     */
    private static List<Patient> seedPatients(PatientService service, List<PatientSeed> seeds)
            throws InvalidDataException {
        List<Patient> created = new ArrayList<>();
        long contact = 9810000001L;
        for (PatientSeed s : seeds) {
            Patient p = service.addPatient(s.name(), s.age(), String.valueOf(contact++),
                    s.bloodGroup(), s.insured());
            s.history().forEach(p::addMedicalHistoryEntry);
            s.allergies().forEach(p::addAllergy);
            created.add(p);
        }
        return created;
    }

    /** The twenty-four hand-written records, including the team in patient form. */
    private static List<PatientSeed> featuredPatients() {
        return List.of(
                // The project team — initials-first format, distinct from the doctor roster.
                new PatientSeed("B A Sunil Kumar", 29, "O+", true,
                        List.of("Annual health check 2024 — all markers normal",
                                "Vitamin D deficiency, supplements prescribed 2025"),
                        List.of("Dust mites"), Specialization.ENDOCRINOLOGY),
                new PatientSeed("P S Varun", 31, "B+", true,
                        List.of("Sports injury — left ankle sprain 2023, fully recovered",
                                "Routine cardiac screening 2025, ECG normal"),
                        List.of(NO_ALLERGIES), Specialization.ORTHOPEDICS),
                new PatientSeed("Ahmed Zubair", 28, "A+", false,
                        List.of("Seasonal allergic rhinitis, recurring each spring",
                                "Mild asthma diagnosed 2019, inhaler as needed"),
                        List.of("Pollen", "Dust"), Specialization.PULMONOLOGY),
                new PatientSeed("Sharma Anushtha", 26, "AB+", true,
                        List.of("Migraine with aura, first episode 2022",
                                "Screen-related eye strain noted 2025"),
                        List.of("Bright light sensitivity"), Specialization.NEUROLOGY),

                new PatientSeed("Ravi Kumar", 67, "O+", false,
                        List.of("Hypertension diagnosed 2021, on amlodipine",
                                "Coronary angiogram 2023 — mild stenosis, medically managed",
                                "Cholesterol elevated, statin started 2024"),
                        List.of("Penicillin"), Specialization.CARDIOLOGY),
                new PatientSeed("Meera Joshi", 34, "A+", true,
                        List.of("Migraine, recurring since 2018",
                                "Iron deficiency anaemia 2024, resolved"),
                        List.of("Sulfa drugs"), Specialization.NEUROLOGY),
                new PatientSeed("Aditya Verma", 8, "B+", true,
                        List.of("Routine vaccinations complete to schedule",
                                "Recurrent tonsillitis 2025 — three episodes"),
                        List.of("Peanuts", "Tree nuts"), Specialization.PEDIATRICS),
                new PatientSeed("Fatima Sheikh", 45, "AB+", false,
                        List.of("Type 2 diabetes diagnosed 2020, metformin",
                                "Diabetic retinopathy screening 2025 — clear"),
                        List.of("Latex"), Specialization.ENDOCRINOLOGY),
                new PatientSeed("Karan Singh", 72, "O-", false,
                        List.of("Osteoarthritis, both knees, diagnosed 2019",
                                "Cataract surgery right eye 2023",
                                "Hearing loss, age-related, hearing aid fitted 2024"),
                        List.of("Aspirin"), Specialization.ORTHOPEDICS),
                new PatientSeed("Ananya Reddy", 27, "A-", true,
                        List.of("PCOS diagnosed 2023, lifestyle managed"),
                        List.of(NO_ALLERGIES), Specialization.GYNECOLOGY),
                new PatientSeed("Mohammed Ali", 53, "B-", true,
                        List.of("Chronic acidity, endoscopy 2024 — mild gastritis",
                                "H. pylori treated 2024"),
                        List.of("NSAIDs"), Specialization.GASTROENTEROLOGY),
                new PatientSeed("Sita Lakshmi", 61, "O+", false,
                        List.of("Hypothyroidism since 2015, thyroxine daily",
                                "Osteopenia noted on DEXA 2024"),
                        List.of("Iodine contrast"), Specialization.ENDOCRINOLOGY),
                new PatientSeed("Rohit Malhotra", 39, "AB-", true,
                        List.of("Anxiety disorder diagnosed 2022, therapy ongoing",
                                "Insomnia, sleep hygiene programme 2024"),
                        List.of(NO_ALLERGIES), Specialization.PSYCHIATRY),
                new PatientSeed("Priyanka Das", 30, "A+", true,
                        List.of("First pregnancy 2025, routine antenatal care"),
                        List.of("Shellfish"), Specialization.GYNECOLOGY),
                new PatientSeed("Vijay Anand", 44, "B+", false,
                        List.of("Chronic sinusitis, septoplasty 2021",
                                "Recurrent throat infections, winters"),
                        List.of("Dust", "Smoke"), Specialization.ENT),
                new PatientSeed("Nandini Rao", 12, "O+", true,
                        List.of("Asthma diagnosed 2021, well controlled",
                                "Growth and development on track"),
                        List.of("Pollen", "Cats"), Specialization.PEDIATRICS),
                new PatientSeed("Suresh Babu", 58, "A+", false,
                        List.of("Smoker 30 years, quit 2023",
                                "COPD diagnosed 2024, inhaler twice daily",
                                "Annual spirometry — declining slowly"),
                        List.of(NO_ALLERGIES), Specialization.PULMONOLOGY),
                new PatientSeed("Zara Hussain", 22, "B+", true,
                        List.of("Severe acne 2024, isotretinoin course completed"),
                        List.of("Benzoyl peroxide"), Specialization.DERMATOLOGY),
                new PatientSeed("Gopal Krishna", 69, "O+", false,
                        List.of("Glaucoma diagnosed 2022, drops daily",
                                "Cataract left eye, surgery scheduled"),
                        List.of("Timolol — mild reaction"), Specialization.OPHTHALMOLOGY),
                new PatientSeed("Ishita Banerjee", 35, "AB+", true,
                        List.of("Impacted wisdom tooth extracted 2024",
                                "Gum disease, deep cleaning 2025"),
                        List.of("Local anaesthetic — mild"), Specialization.DENTISTRY),
                new PatientSeed("Harish Patel", 47, "A-", false,
                        List.of("Lower back pain, disc bulge L4-L5 on MRI 2023",
                                "Physiotherapy course completed 2024"),
                        List.of("Codeine"), Specialization.ORTHOPEDICS),
                new PatientSeed("Leela Menon", 78, "O-", false,
                        List.of("Atrial fibrillation diagnosed 2020, anticoagulated",
                                "Mild cognitive impairment noted 2024",
                                "Fall risk assessment 2025 — moderate"),
                        List.of("Warfarin interactions — monitored"), Specialization.CARDIOLOGY),
                new PatientSeed("Tanvi Shah", 5, "B+", true,
                        List.of("Born full term, no complications",
                                "Vaccinations up to date",
                                "Mild eczema, emollients"),
                        List.of("Eggs"), Specialization.PEDIATRICS),
                new PatientSeed("Devendra Yadav", 41, "A+", true,
                        List.of("Kidney stone passed 2023",
                                "Recurrent acidity, dietary management"),
                        List.of(NO_ALLERGIES), Specialization.GASTROENTEROLOGY));
    }

    /**
     * Forty-eight further patients composed from name and condition pools.
     *
     * <p>Composed rather than hand-written, but never blank — each still gets a
     * dated history entry, an allergy record and a coherent speciality need.</p>
     */
    private static List<PatientSeed> generatedPatients() {
        String[] firstNames = {
                "Aarav", "Diya", "Kabir", "Ishaan", "Saanvi", "Reyansh", "Myra", "Advait",
                "Aditi", "Vihaan", "Anaya", "Krish", "Pooja", "Nikhil", "Shreya", "Manav",
                "Divya", "Rakesh", "Kiran", "Sunita", "Naveen", "Rekha", "Ajay", "Sneha"};
        String[] lastNames = {
                "Sharma", "Verma", "Nair", "Rao", "Iyer", "Gupta", "Bose", "Chopra",
                "Mishra", "Pandey", "Sinha", "Kaur"};
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        String[][] conditions = {
                {"Seasonal flu, treated 2024", "Dust", "GENERAL_PRACTICE"},
                {"Vitamin B12 deficiency, supplements 2025", NO_ALLERGIES, "GENERAL_PRACTICE"},
                {"Hypertension stage 1, lifestyle managed 2024", NO_ALLERGIES, "CARDIOLOGY"},
                {"Palpitations investigated 2025, Holter normal", "Caffeine sensitivity", "CARDIOLOGY"},
                {"Tension headaches, recurring 2024", NO_ALLERGIES, "NEUROLOGY"},
                {"Contact dermatitis 2025, topical steroid", "Nickel", "DERMATOLOGY"},
                {"Frozen shoulder 2024, physiotherapy", NO_ALLERGIES, "ORTHOPEDICS"},
                {"Ankle fracture 2023, healed", "Ibuprofen", "ORTHOPEDICS"},
                {"Allergic rhinitis, perennial", "Pollen", "ENT"},
                {"Myopia, spectacles since 2020", NO_ALLERGIES, "OPHTHALMOLOGY"},
                {"Work-related stress 2025, counselling", NO_ALLERGIES, "PSYCHIATRY"},
                {"Routine dental scaling 2025", NO_ALLERGIES, "DENTISTRY"},
                {"Mild asthma, exercise induced", "Cold air", "PULMONOLOGY"},
                {"Irritable bowel syndrome 2024, dietary", "Lactose", "GASTROENTEROLOGY"},
                {"Prediabetes flagged 2025, diet and exercise", NO_ALLERGIES, "ENDOCRINOLOGY"},
                {"Thyroid nodule, benign on biopsy 2024", NO_ALLERGIES, "ENDOCRINOLOGY"}};

        Random random = new Random(RANDOM_SEED);
        List<PatientSeed> out = new ArrayList<>();

        for (int i = 0; i < 48; i++) {
            String name = firstNames[i % firstNames.length] + " "
                    + lastNames[(i * 5 + 3) % lastNames.length];
            int age = 3 + random.nextInt(82);
            String[] condition = conditions[random.nextInt(conditions.length)];

            List<String> history = new ArrayList<>();
            history.add(condition[0]);
            history.add("Registered with MediTrack, record opened 2026");
            if (age >= 60) {
                history.add("Senior wellness review scheduled annually");
            } else if (age <= 12) {
                history.add("Paediatric growth chart maintained");
            }

            out.add(new PatientSeed(name, age,
                    bloodGroups[random.nextInt(bloodGroups.length)],
                    random.nextInt(100) < 45,
                    List.copyOf(history),
                    List.of(condition[1]),
                    Specialization.valueOf(condition[2])));
        }
        return out;
    }

    // ------------------------------------------------------------------ appointments

    /**
     * Books roughly ninety appointments and moves them through every lifecycle
     * state, so the status breakdown report has something to break down.
     *
     * <p>Every slot is in the future — the service refuses a past booking, by
     * design — and the clinic's own rules still apply: 09:00 to 17:00, 30-minute
     * slots, at most eight per doctor per day. The seeder respects those rather
     * than bypassing them, which is the point: <b>the demo data is produced
     * through the same validated API a user goes through.</b></p>
     */
    private static List<Appointment> seedAppointments(AppointmentService appointments,
                                                      List<Doctor> doctors,
                                                      List<Patient> patients,
                                                      List<PatientSeed> seeds) {
        LocalDateTime base = DateUtil.nowAlignedToSlot().plusDays(1)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);

        List<Appointment> booked = new ArrayList<>();
        Random random = new Random(RANDOM_SEED);
        int[] perDoctorPerDay = new int[doctors.size() * 6];

        for (int i = 0; i < patients.size() && booked.size() < 92; i++) {
            Patient patient = patients.get(i);

            // Route each patient to a doctor who actually treats their condition.
            int doctorIndex = pickDoctorFor(doctors, seeds.get(i).typicalNeed(), random);
            Doctor doctor = doctors.get(doctorIndex);

            int day = i % 5;
            int slotIndex = perDoctorPerDay[doctorIndex * 6 + day];
            if (slotIndex >= Constants.MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY) {
                continue;
            }
            perDoctorPerDay[doctorIndex * 6 + day] = slotIndex + 1;

            LocalDateTime slot = base.plusDays(day).plusMinutes(
                    (long) slotIndex * Constants.SLOT_DURATION_MINUTES * 2L);
            if (slot.getHour() >= Constants.CLINIC_CLOSE_HOUR) {
                continue;
            }

            Appointment appointment = book(appointments, patient, doctor, slot,
                    symptomsFor(doctor.getSpecialization()));
            if (appointment != null) {
                booked.add(appointment);
            }
        }

        advanceLifecycles(appointments, booked, random);
        return booked;
    }

    /**
     * Finds a doctor who actually treats the patient's condition.
     *
     * <p>Scanning from a random offset rather than always from index 0 matters:
     * two doctors share most specialities here, and starting from the front every
     * time would give the first of each pair the entire caseload while the second
     * sat idle. The AI triage ranking penalises current bookings, so a lopsided
     * seed would make its output look broken.
     *
     * <p>Falls back to any doctor if the speciality is unstaffed.
     */
    private static int pickDoctorFor(List<Doctor> doctors, Specialization need, Random random) {
        int start = random.nextInt(doctors.size());
        for (int offset = 0; offset < doctors.size(); offset++) {
            int candidate = (start + offset) % doctors.size();
            if (doctors.get(candidate).getSpecialization() == need) {
                return candidate;
            }
        }
        return start;
    }

    /** Two representative symptoms for a speciality, drawn from its own keywords. */
    private static List<String> symptomsFor(Specialization specialization) {
        List<String> keywords = specialization.getSymptomKeywords();
        if (keywords.size() < 2) {
            return List.copyOf(keywords);
        }
        return List.of(keywords.get(0), keywords.get(1));
    }

    /**
     * Moves the booked appointments through confirm, complete and cancel so the
     * dataset is not uniformly PENDING.
     *
     * <p>Target mix: roughly half completed, a quarter confirmed, a tenth
     * cancelled, the rest left pending — which is what a real clinic's Monday
     * morning looks like.</p>
     */
    private static void advanceLifecycles(AppointmentService service,
                                          List<Appointment> booked,
                                          Random random) {
        for (int i = 0; i < booked.size(); i++) {
            String id = booked.get(i).getId();
            int roll = random.nextInt(100);
            try {
                if (roll < 50) {
                    service.confirmAppointment(id);
                    service.completeAppointment(id);
                } else if (roll < 75) {
                    service.confirmAppointment(id);
                } else if (roll < 85) {
                    service.cancelAppointment(id);
                }
                // remainder stays PENDING
            } catch (Exception e) {
                // A lifecycle miss must never abort seeding — the record simply
                // stays in whatever state it reached.
            }
        }
    }

    /**
     * Maps a 0-99 roll onto a bill type, weighted like a real clinic's mix:
     * mostly consultations, some procedures, emergencies rare.
     */
    private static BillType pickBillType(int roll) {
        if (roll < 70) {
            return BillType.CONSULTATION;
        }
        if (roll < 90) {
            return BillType.PROCEDURE;
        }
        return BillType.EMERGENCY;
    }

    /** Books one appointment, swallowing failures so a clash cannot abort seeding. */
    private static Appointment book(AppointmentService service, Patient patient, Doctor doctor,
                                    LocalDateTime slot, List<String> symptoms) {
        try {
            return service.bookAppointment(patient, doctor, slot, symptoms);
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ billing

    /**
     * Raises a bill for every completed appointment and settles them unevenly.
     *
     * <p>Uneven on purpose. A dataset where every bill is paid makes the
     * "unpaid bills" screen and the outstanding-revenue figure both useless, and
     * those are exactly the screens a reviewer wants to see populated.</p>
     *
     * @return the number of bills raised
     */
    private static int seedBills(BillingService billing, List<Appointment> appointments) {
        Random random = new Random(RANDOM_SEED);
        int raised = 0;

        for (Appointment appointment : appointments) {
            if (!appointment.getStatus().name().equals("COMPLETED")) {
                continue;
            }
            try {
                var bill = billing.generateBillForAppointment(
                        appointment, pickBillType(random.nextInt(100)));
                raised++;

                // Settle roughly half in full, a third partially, leave the rest open.
                int payRoll = random.nextInt(100);
                if (payRoll < 50) {
                    billing.settleInFull(bill.getBillId());
                } else if (payRoll < 80) {
                    billing.recordPayment(bill.getBillId(), bill.getTotalAmount() / 2);
                }
            } catch (Exception e) {
                // Billing is demo garnish; a miss must not abort seeding.
            }
        }
        return raised;
    }
}
