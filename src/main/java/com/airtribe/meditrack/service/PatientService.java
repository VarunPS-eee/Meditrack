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

public class PatientService {

    private final DataStore<Patient> store;
    private final IdGenerator idGenerator;

    public PatientService() {
        this(new DataStore<>("Patient"));
    }

    public PatientService(DataStore<Patient> store) {
        this.store = store;
        this.idGenerator = IdGenerator.getInstance();
    }

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

    public Patient register(Patient patient) {
        if (patient != null) {
            idGenerator.syncFromId(patient.getId());
        }
        return store.save(patient);
    }

    public Optional<Patient> findById(String id) {
        return store.findById(id);
    }

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

    public List<Patient> searchPatient(String keyword) {
        return store.search(keyword);
    }

    public List<Patient> searchPatient(int age) {
        return store.findBy(p -> p.getAge() == age);
    }

    public List<Patient> searchPatient(int minAge, int maxAge) {
        return store.findBy(p -> p.getAge() >= minAge && p.getAge() <= maxAge);
    }

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

    public void addMedicalHistory(String id, String entry)
            throws EntityNotFoundException, InvalidDataException {
        Patient patient = store.getById(id);
        patient.addMedicalHistoryEntry(Validator.requireNonBlank(entry, "historyEntry"));
    }

    public void addAllergy(String id, String allergy)
            throws EntityNotFoundException, InvalidDataException {
        Patient patient = store.getById(id);
        patient.addAllergy(Validator.requireNonBlank(allergy, "allergy"));
    }

    public boolean deletePatient(String id) {
        return store.deleteById(id);
    }

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

    public List<String> getAllKnownAllergies() {
        return store.stream()
                .flatMap(p -> p.getAllergies().stream())
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * @throws DataPersistenceException if the CSV cannot be written
     */
    public void saveToFile() throws DataPersistenceException {
        CSVUtil.writePatients(Constants.PATIENTS_FILE, store.findAll());
    }

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
