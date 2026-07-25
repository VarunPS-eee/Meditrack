package com.airtribe.meditrack.service;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.DataPersistenceException;
import com.airtribe.meditrack.exception.EntityNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.CSVUtil;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All patient use-cases: create, read, update, delete and search.
 *
 * <h2>Overloading</h2>
 * <p>{@code searchPatient} is overloaded four ways — by id, by name, by age and by age
 * range. All four are resolved at <b>compile time</b> from the argument types, which is
 * the distinction from overriding: the compiler picks the method, the JVM does not.</p>
 *
 * <p>Note {@code searchPatient(String)} and {@code searchPatientById(String)} are
 * deliberately <em>not</em> both overloads on {@code String} — two methods differing only
 * in intent, not in signature, cannot be overloaded, so they get distinct names. Java
 * chooses by type, not by what you meant.</p>
 *
 * <h2>SOLID</h2>
 * <p>Single Responsibility: this class orchestrates patient use-cases. It does not
 * validate ({@link Validator}), does not store ({@link DataStore}) and does not
 * format ({@link Patient#displayDetails()}).</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
public class PatientService {

    private final DataStore<Patient> store;
    private final IdGenerator idGenerator;

    public PatientService() {
        this(new DataStore<>("Patient"));
    }

    /**
     * Constructor injection — the store is a collaborator, not a hidden global. This is
     * what makes {@code TestRunner} able to hand in a clean store per test.
     *
     * @param store the backing store
     */
    public PatientService(DataStore<Patient> store) {
        this.store = store;
        this.idGenerator = IdGenerator.getInstance();
    }

    // -------------------------------------------------------------------- create
    /**
     * Registers a new patient, validating every field first.
     *
     * @param name          full name
     * @param age           age in years
     * @param contactNumber 10-digit contact number
     * @param bloodGroup    blood group, may be {@code null}
     * @param insured       whether the patient carries insurance
     * @return the newly created patient
     * @throws InvalidDataException if any field fails validation
     */
    public Patient addPatient(String name, int age, String contactNumber,
                              String bloodGroup, boolean insured) throws InvalidDataException {
        String validName = Validator.validateName(name);
        int validAge = Validator.validateAge(age);
        String validContact = Validator.validateContactNumber(contactNumber);
        String validBloodGroup = Validator.validateBloodGroup(bloodGroup);

        Patient patient = new Patient(idGenerator.nextPatientId(), validName, validAge,
                validContact, List.of(), List.of(), validBloodGroup, insured);
        return store.save(patient);
    }

    /** Convenience overload for the common case with no blood group or insurance. */
    public Patient addPatient(String name, int age, String contactNumber) throws InvalidDataException {
        return addPatient(name, age, contactNumber, null, false);
    }

    /**
     * Adds an already-built patient — used when importing from disk, where ids come from
     * the file rather than the generator.
     *
     * @param patient the patient to register
     * @return the stored patient
     */
    public Patient register(Patient patient) {
        if (patient != null) {
            idGenerator.syncFromId(patient.getId());
        }
        return store.save(patient);
    }

    // ---------------------------------------------------------------------- read
    public Optional<Patient> findById(String id) {
        return store.findById(id);
    }

    /**
     * @param id the patient id
     * @return the patient
     * @throws EntityNotFoundException if no patient has that id
     */
    public Patient getById(String id) throws EntityNotFoundException {
        return store.getById(id);
    }

    public List<Patient> getAllPatients() {
        return store.findAll();
    }

    public List<Patient> getAllSortedByName() {
        return store.findAllSorted(Comparator.comparing(Patient::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public List<Patient> getAllSortedByAge() {
        return store.findAllSorted(Comparator.comparingInt(Patient::getAge).reversed());
    }

    public int count() {
        return store.count();
    }

    // ------------------------------------------------------------- OVERLOADING
    /**
     * Overload 1 — free-text search across id, name, history, allergies and blood group.
     *
     * @param keyword the search term
     * @return the matching patients
     */
    public List<Patient> searchPatient(String keyword) {
        return store.search(keyword);
    }

    /**
     * Overload 2 — exact age match.
     *
     * @param age the age to match
     * @return patients of exactly that age
     */
    public List<Patient> searchPatient(int age) {
        return store.findBy(p -> p.getAge() == age);
    }

    /**
     * Overload 3 — inclusive age range.
     *
     * @param minAge lower bound, inclusive
     * @param maxAge upper bound, inclusive
     * @return patients within the range
     */
    public List<Patient> searchPatient(int minAge, int maxAge) {
        return store.findBy(p -> p.getAge() >= minAge && p.getAge() <= maxAge);
    }

    /**
     * Overload 4 — search by name, optionally requiring an exact match.
     *
     * @param name       the name to look for
     * @param exactMatch {@code true} for equality, {@code false} for "contains"
     * @return the matching patients
     */
    public List<Patient> searchPatient(String name, boolean exactMatch) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        String needle = name.trim().toLowerCase();
        return store.findBy(p -> exactMatch
                ? p.getName().equalsIgnoreCase(needle)
                : p.getName().toLowerCase().contains(needle));
    }

    /** Distinct name, not an overload — see the class note on why. */
    public Optional<Patient> searchPatientById(String id) {
        return store.findById(id);
    }

    // -------------------------------------------------------------------- update
    /**
     * Updates a patient's mutable details. Only non-null arguments are applied, so a
     * caller can change one field without resupplying the rest.
     *
     * @param id            the patient to update
     * @param name          new name, or {@code null} to leave unchanged
     * @param age           new age, or {@code null} to leave unchanged
     * @param contactNumber new contact number, or {@code null} to leave unchanged
     * @return the updated patient
     * @throws EntityNotFoundException if no patient has that id
     * @throws InvalidDataException    if a supplied value fails validation
     */
    public Patient updatePatient(String id, String name, Integer age, String contactNumber)
            throws EntityNotFoundException, InvalidDataException {
        Patient patient = store.getById(id);

        if (name != null && !name.isBlank()) {
            patient.setName(Validator.validateName(name));
        }
        if (age != null) {
            patient.setAge(Validator.validateAge(age));
        }
        if (contactNumber != null && !contactNumber.isBlank()) {
            patient.setContactNumber(Validator.validateContactNumber(contactNumber));
        }
        return store.save(patient);
    }

    /**
     * Appends a line to a patient's medical history.
     *
     * @param id    the patient
     * @param entry the history note
     * @throws EntityNotFoundException if no patient has that id
     * @throws InvalidDataException    if the note is blank
     */
    public void addMedicalHistory(String id, String entry)
            throws EntityNotFoundException, InvalidDataException {
        Patient patient = store.getById(id);
        patient.addMedicalHistoryEntry(Validator.requireNonBlank(entry, "historyEntry"));
    }

    /**
     * Records a known allergy.
     *
     * @param id      the patient
     * @param allergy the allergy
     * @throws EntityNotFoundException if no patient has that id
     * @throws InvalidDataException    if the allergy is blank
     */
    public void addAllergy(String id, String allergy)
            throws EntityNotFoundException, InvalidDataException {
        Patient patient = store.getById(id);
        patient.addAllergy(Validator.requireNonBlank(allergy, "allergy"));
    }

    // -------------------------------------------------------------------- delete
    /**
     * @param id the patient to remove
     * @return {@code true} if a patient was removed
     */
    public boolean deletePatient(String id) {
        return store.deleteById(id);
    }

    // ------------------------------------------------------- streams & analytics
    /**
     * Groups patients into age bands.
     *
     * @return band name to patient count
     */
    public Map<String, Long> countByAgeGroup() {
        return store.stream()
                .collect(Collectors.groupingBy(Patient::getAgeGroup, Collectors.counting()));
    }

    /**
     * @return the mean patient age, or {@code 0.0} when there are none
     */
    public double getAverageAge() {
        return store.stream().mapToInt(Patient::getAge).average().orElse(0.0);
    }

    /**
     * @return patients aged {@value Constants#SENIOR_CITIZEN_AGE} and over
     */
    public List<Patient> getSeniorCitizens() {
        return store.findBy(Patient::isSeniorCitizen);
    }

    /**
     * @return patients carrying insurance
     */
    public List<Patient> getInsuredPatients() {
        return store.findBy(Patient::isInsured);
    }

    /**
     * Every distinct allergy on file, sorted — a flat-map over nested collections.
     *
     * @return the distinct allergies
     */
    public List<String> getAllKnownAllergies() {
        return store.stream()
                .flatMap(p -> p.getAllergies().stream())
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------- persistence
    /**
     * @throws DataPersistenceException if the CSV cannot be written
     */
    public void saveToFile() throws DataPersistenceException {
        CSVUtil.writePatients(Constants.PATIENTS_FILE, store.findAll());
    }

    /**
     * @return how many patients were loaded
     * @throws DataPersistenceException if the CSV cannot be read
     */
    public int loadFromFile() throws DataPersistenceException {
        List<Patient> loaded = CSVUtil.readPatients(Constants.PATIENTS_FILE);
        loaded.forEach(this::register);
        return loaded.size();
    }

    /** @return patients keyed by id, for re-linking appointments after a load */
    public Map<String, Patient> asMap() {
        return store.stream().collect(Collectors.toMap(Patient::getId, p -> p));
    }

    public DataStore<Patient> getStore() {
        return store;
    }

    /** Prints every patient using polymorphic {@code displayDetails()}. */
    public void displayAll() {
        if (store.isEmpty()) {
            System.out.println("  No patients registered yet.");
            return;
        }
        System.out.println("\n  Patients (" + store.count() + ")");
        System.out.println("  " + "-".repeat(76));
        for (Patient patient : store) {   // for-each over DataStore — it is Iterable
            patient.displayDetails();
        }
    }
}
