package com.airtribe.meditrack.service;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.DataPersistenceException;
import com.airtribe.meditrack.exception.EntityNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.CSVUtil;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DoctorService {

    private final DataStore<Doctor> store;
    private final IdGenerator idGenerator;

    public DoctorService() {
        this(new DataStore<>("Doctor"));
    }

    public DoctorService(DataStore<Doctor> store) {
        this.store = store;
        this.idGenerator = IdGenerator.getInstance();
    }

    public Doctor addDoctor(String name, int age, String contactNumber,
                            Specialization specialization, double consultationFee,
                            int yearsOfExperience) throws InvalidDataException {
        String validName = Validator.validateName(name);
        int validAge = Validator.validateAge(age);
        String validContact = Validator.validateContactNumber(contactNumber);
        double validFee = Validator.validateFee(consultationFee, "consultationFee");
        Specialization validSpec = Validator.requireNonNull(specialization, "specialization");

        Doctor doctor = new Doctor(idGenerator.nextDoctorId(), validName, validAge, validContact,
                validSpec, validFee, Math.max(0, yearsOfExperience), 0.0);
        return store.save(doctor);
    }

    /** Convenience overload that takes the speciality's default fee. */
    public Doctor addDoctor(String name, int age, String contactNumber,
                            Specialization specialization) throws InvalidDataException {
        return addDoctor(name, age, contactNumber, specialization,
                specialization == null ? 0 : specialization.getBaseConsultationFee(), 0);
    }

    /** Registers a pre-built doctor, syncing the id counter — used when importing. */
    public Doctor register(Doctor doctor) {
        if (doctor != null) {
            idGenerator.syncFromId(doctor.getId());
        }
        return store.save(doctor);
    }

    public Optional<Doctor> findById(String id) {
        return store.findById(id);
    }

    public Doctor getById(String id) throws EntityNotFoundException {
        return store.getById(id);
    }

    public List<Doctor> getAllDoctors() {
        return store.findAll();
    }

    public int count() {
        return store.count();
    }

    public List<Doctor> searchDoctor(String keyword) {
        return store.search(keyword);
    }

    public List<Doctor> searchDoctor(Specialization specialization) {
        return store.findBy(d -> d.getSpecialization() == specialization);
    }

    public List<Doctor> searchDoctor(double maxFee) {
        return store.stream()
                .filter(d -> d.getConsultationFee() <= maxFee)
                .sorted(Doctor.BY_FEE)
                .collect(Collectors.toList());
    }

    public List<Doctor> searchDoctor(Specialization specialization, int minYearsExperience) {
        return store.stream()
                .filter(d -> d.getSpecialization() == specialization)
                .filter(d -> d.getYearsOfExperience() >= minYearsExperience)
                .sorted(Doctor.BY_EXPERIENCE)
                .collect(Collectors.toList());
    }

    /** @return only doctors currently accepting appointments */
    public List<Doctor> getAvailableDoctors() {
        return store.findBy(Doctor::isAvailable);
    }

    public List<Doctor> getAvailableBySpecialization(Specialization specialization) {
        return store.stream()
                .filter(Doctor::isAvailable)
                .filter(d -> d.getSpecialization() == specialization)
                .sorted(Doctor.BY_RATING)
                .collect(Collectors.toList());
    }

    public Doctor updateDoctor(String id, String name, Double consultationFee, Boolean available)
            throws EntityNotFoundException, InvalidDataException {
        Doctor doctor = store.getById(id);

        if (name != null && !name.isBlank()) {
            doctor.setName(Validator.validateName(name));
        }
        if (consultationFee != null) {
            doctor.setConsultationFee(Validator.validateFee(consultationFee, "consultationFee"));
        }
        if (available != null) {
            doctor.setAvailable(available);
        }
        return store.save(doctor);
    }

    public void rateDoctor(String id, double rating) throws EntityNotFoundException {
        Doctor doctor = store.getById(id);
        double clamped = Math.clamp(rating, 0.0, 5.0);
        double current = doctor.getRating();
        doctor.setRating(current == 0.0 ? clamped : (current + clamped) / 2.0);
    }

    public boolean deleteDoctor(String id) {
        return store.deleteById(id);
    }

    public Map<Specialization, Double> getAverageFeeBySpecialization() {
        return store.stream()
                .filter(d -> d.getSpecialization() != null)
                .collect(Collectors.groupingBy(
                        Doctor::getSpecialization,
                        java.util.TreeMap::new,
                        Collectors.averagingDouble(Doctor::getConsultationFee)));
    }

    /**
     * @return the overall mean consultation fee, or {@code 0.0} with no doctors
     */
    public double getOverallAverageFee() {
        return store.stream().mapToDouble(Doctor::getConsultationFee).average().orElse(0.0);
    }

    /**
     * @return min / max / mean / count of consultation fees in one pass
     */
    public DoubleSummaryStatistics getFeeStatistics() {
        return store.stream().mapToDouble(Doctor::getConsultationFee).summaryStatistics();
    }

    /**
     * @return how many doctors hold each speciality
     */
    public Map<Specialization, Long> countBySpecialization() {
        return store.stream()
                .filter(d -> d.getSpecialization() != null)
                .collect(Collectors.groupingBy(
                        Doctor::getSpecialization,
                        java.util.TreeMap::new,
                        Collectors.counting()));
    }

    public List<Doctor> getTopRatedDoctors(int limit) {
        return store.stream()
                .sorted(Doctor.BY_RATING)
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /**
     * @return the most expensive doctor on the roster, if any
     */
    public Optional<Doctor> getMostExpensiveDoctor() {
        return store.stream().max(Comparator.comparingDouble(Doctor::getConsultationFee));
    }

    /**
     * @return specialities with nobody assigned — a staffing gap report
     */
    public List<Specialization> getUnstaffedSpecializations() {
        List<Specialization> staffed = store.stream()
                .map(Doctor::getSpecialization)
                .distinct()
                .toList();
        return java.util.Arrays.stream(Specialization.values())
                .filter(s -> !staffed.contains(s))
                .collect(Collectors.toList());
    }

    public void saveToFile() throws DataPersistenceException {
        CSVUtil.writeDoctors(Constants.DOCTORS_FILE, store.findAll());
    }

    public int loadFromFile() throws DataPersistenceException {
        List<Doctor> loaded = CSVUtil.readDoctors(Constants.DOCTORS_FILE);
        loaded.forEach(this::register);
        return loaded.size();
    }

    /** @return doctors keyed by id, for re-linking appointments after a load */
    public Map<String, Doctor> asMap() {
        return store.stream().collect(Collectors.toMap(Doctor::getId, d -> d));
    }

    public DataStore<Doctor> getStore() {
        return store;
    }

    /** Prints every doctor, best-rated first. */
    public void displayAll() {
        if (store.isEmpty()) {
            System.out.println("  No doctors on the roster yet.");
            return;
        }
        System.out.println("\n  Doctors (" + store.count() + ")");
        System.out.println("  " + "-".repeat(110));
        store.findAllSorted(Doctor.BY_RATING).forEach(Doctor::displayDetails);
    }
}
