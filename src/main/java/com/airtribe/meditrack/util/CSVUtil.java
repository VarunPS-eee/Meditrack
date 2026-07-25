package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.DataPersistenceException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CSV reading and writing for patients, doctors and appointments.
 *
 * <h2>try-with-resources</h2>
 * <p>Every file operation here opens its reader or writer in a try-with-resources header.
 * The resource is closed automatically on both the success and the exception path, which
 * is what the old {@code finally { if (r != null) r.close(); }} dance existed to achieve —
 * and frequently got wrong.</p>
 *
 * <h2>Escaping</h2>
 * <p>Fields are split on {@code ,} with {@code String.split(",")} as the assignment
 * requires. Because a medical-history entry can legitimately contain a comma, values are
 * escaped on the way out — commas become {@code ||} — and unescaped on the way back in.
 * A blunt split would otherwise shift every subsequent column.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class CSVUtil {

    public static final String PATIENT_HEADER =
            "id,name,age,contactNumber,bloodGroup,insured,medicalHistory,allergies";
    public static final String DOCTOR_HEADER =
            "id,name,age,contactNumber,specialization,consultationFee,yearsOfExperience,rating,available";
    public static final String APPOINTMENT_HEADER =
            "id,patientId,doctorId,slot,status,symptoms,notes";

    private CSVUtil() {
        throw new AssertionError("CSVUtil is a utility class and must not be instantiated.");
    }

    // ------------------------------------------------------------------ escaping
    /**
     * Makes a value safe to place between commas.
     *
     * @param value the raw value, may be {@code null}
     * @return the escaped value, never {@code null}
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", "||").replace("\n", " ").replace("\r", " ").trim();
    }

    /**
     * Reverses {@link #escape(String)}.
     *
     * @param value the escaped value
     * @return the original value, or {@code null} if blank
     */
    public static String unescape(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replace("||", ",").trim();
    }

    /**
     * Joins a list into one CSV cell using {@code ;}.
     *
     * @param values the values to join
     * @return the joined, escaped cell
     */
    public static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return escape(String.join(Constants.LIST_DELIMITER, values));
    }

    /**
     * Splits a {@code ;}-joined cell back into a list.
     *
     * @param cell the cell contents
     * @return the parsed values, never {@code null}
     */
    public static List<String> splitList(String cell) {
        String unescaped = unescape(cell);
        if (unescaped == null || unescaped.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(unescaped.split(Constants.LIST_DELIMITER)));
    }

    // -------------------------------------------------------------------- write
    /**
     * Writes a header plus rows to a CSV file, creating the directory if needed.
     *
     * @param filePath where to write
     * @param header   the header line
     * @param rows     the data lines
     * @throws DataPersistenceException if the write fails
     */
    public static void writeLines(String filePath, String header, List<String> rows)
            throws DataPersistenceException {
        ensureParentDirectory(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8)) {

            writer.write(header);
            writer.newLine();
            for (String row : rows) {
                writer.write(row);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new DataPersistenceException(filePath, "Failed to write CSV", e);
        }
    }

    /**
     * Reads every data line from a CSV file, skipping the header.
     *
     * @param filePath the file to read
     * @return the data lines; empty if the file does not exist
     * @throws DataPersistenceException if the file exists but cannot be read
     */
    public static List<String> readLines(String filePath) throws DataPersistenceException {
        List<String> lines = new ArrayList<>();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return lines;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;   // discard header
                    continue;
                }
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            throw new DataPersistenceException(filePath, "Failed to read CSV", e);
        }
        return lines;
    }

    // ------------------------------------------------------------------ patients
    /**
     * @param patient the patient to serialise
     * @return one CSV row
     */
    public static String toCsvRow(Patient patient) {
        return String.join(Constants.CSV_DELIMITER,
                escape(patient.getId()),
                escape(patient.getName()),
                String.valueOf(patient.getAge()),
                escape(patient.getContactNumber()),
                escape(patient.getBloodGroup()),
                String.valueOf(patient.isInsured()),
                joinList(patient.getMedicalHistory()),
                joinList(patient.getAllergies()));
    }

    /**
     * Parses a patient row.
     *
     * @param row the CSV line
     * @return the patient, or {@code null} if the row is malformed
     */
    public static Patient patientFromCsvRow(String row) {
        String[] f = row.split(Constants.CSV_DELIMITER, -1);
        if (f.length < 8) {
            return null;
        }
        try {
            Patient patient = new Patient(
                    unescape(f[0]),
                    unescape(f[1]),
                    Integer.parseInt(f[2].trim()),
                    unescape(f[3]),
                    splitList(f[6]),
                    splitList(f[7]),
                    unescape(f[4]),
                    Boolean.parseBoolean(f[5].trim()));
            return patient;
        } catch (NumberFormatException e) {
            return null;   // one bad row must not abort the whole import
        }
    }

    public static void writePatients(String filePath, List<Patient> patients)
            throws DataPersistenceException {
        writeLines(filePath, PATIENT_HEADER, mapAll(patients, CSVUtil::toCsvRow));
    }

    public static List<Patient> readPatients(String filePath) throws DataPersistenceException {
        return parseAll(readLines(filePath), CSVUtil::patientFromCsvRow);
    }

    // ------------------------------------------------------------------- doctors
    /**
     * @param doctor the doctor to serialise
     * @return one CSV row
     */
    public static String toCsvRow(Doctor doctor) {
        return String.join(Constants.CSV_DELIMITER,
                escape(doctor.getId()),
                escape(doctor.getName()),
                String.valueOf(doctor.getAge()),
                escape(doctor.getContactNumber()),
                doctor.getSpecialization() == null ? "" : doctor.getSpecialization().name(),
                String.valueOf(doctor.getConsultationFee()),
                String.valueOf(doctor.getYearsOfExperience()),
                String.valueOf(doctor.getRating()),
                String.valueOf(doctor.isAvailable()));
    }

    /**
     * Parses a doctor row.
     *
     * @param row the CSV line
     * @return the doctor, or {@code null} if the row is malformed
     */
    public static Doctor doctorFromCsvRow(String row) {
        String[] f = row.split(Constants.CSV_DELIMITER, -1);
        if (f.length < 9) {
            return null;
        }
        try {
            Doctor doctor = new Doctor(
                    unescape(f[0]),
                    unescape(f[1]),
                    Integer.parseInt(f[2].trim()),
                    unescape(f[3]),
                    Specialization.fromString(f[4]).orElse(Specialization.GENERAL_PRACTICE),
                    Double.parseDouble(f[5].trim()),
                    Integer.parseInt(f[6].trim()),
                    Double.parseDouble(f[7].trim()));
            doctor.setAvailable(Boolean.parseBoolean(f[8].trim()));
            return doctor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void writeDoctors(String filePath, List<Doctor> doctors)
            throws DataPersistenceException {
        writeLines(filePath, DOCTOR_HEADER, mapAll(doctors, CSVUtil::toCsvRow));
    }

    public static List<Doctor> readDoctors(String filePath) throws DataPersistenceException {
        return parseAll(readLines(filePath), CSVUtil::doctorFromCsvRow);
    }

    // -------------------------------------------------------------- appointments
    /**
     * Appointments store only the <em>ids</em> of their patient and doctor, not nested
     * copies. Flattening the object graph this way is what keeps CSV workable — the
     * service layer re-links the references after loading.
     *
     * @param appointment the appointment to serialise
     * @return one CSV row
     */
    public static String toCsvRow(Appointment appointment) {
        return String.join(Constants.CSV_DELIMITER,
                escape(appointment.getId()),
                escape(appointment.getPatient() == null ? "" : appointment.getPatient().getId()),
                escape(appointment.getDoctor() == null ? "" : appointment.getDoctor().getId()),
                DateUtil.format(appointment.getSlot()),
                appointment.getStatus().name(),
                joinList(appointment.getSymptoms()),
                escape(appointment.getNotes()));
    }

    /**
     * Parses an appointment row and re-links it to the supplied entities.
     *
     * @param row      the CSV line
     * @param patients patients by id, for re-linking
     * @param doctors  doctors by id, for re-linking
     * @return the appointment, or {@code null} if malformed or referencing missing entities
     */
    public static Appointment appointmentFromCsvRow(String row,
                                                    Map<String, Patient> patients,
                                                    Map<String, Doctor> doctors) {
        String[] f = row.split(Constants.CSV_DELIMITER, -1);
        if (f.length < 7) {
            return null;
        }
        LocalDateTime slot = DateUtil.parseDateTimeOrNull(f[3]);
        if (slot == null) {
            return null;
        }
        Patient patient = patients.get(unescape(f[1]));
        Doctor doctor = doctors.get(unescape(f[2]));
        if (patient == null || doctor == null) {
            return null;   // orphaned row — referential integrity failure
        }
        return new Appointment(
                unescape(f[0]),
                patient,
                doctor,
                slot,
                AppointmentStatus.fromString(f[4]).orElse(AppointmentStatus.PENDING),
                splitList(f[5]),
                unescape(f[6]));
    }

    public static void writeAppointments(String filePath, List<Appointment> appointments)
            throws DataPersistenceException {
        writeLines(filePath, APPOINTMENT_HEADER, mapAll(appointments, CSVUtil::toCsvRow));
    }

    /**
     * Reads appointments and re-links them against the given patients and doctors.
     *
     * @param filePath the file to read
     * @param patients patients by id
     * @param doctors  doctors by id
     * @return the loaded appointments; rows referencing unknown ids are skipped
     * @throws DataPersistenceException if the file cannot be read
     */
    public static List<Appointment> readAppointments(String filePath,
                                                     Map<String, Patient> patients,
                                                     Map<String, Doctor> doctors)
            throws DataPersistenceException {
        List<Appointment> appointments = new ArrayList<>();
        for (String row : readLines(filePath)) {
            Appointment appointment = appointmentFromCsvRow(row, patients, doctors);
            if (appointment != null) {
                appointments.add(appointment);
            }
        }
        return appointments;
    }

    // ------------------------------------------------------------------ helpers
    private static <E> List<String> mapAll(List<E> items, Function<E, String> mapper) {
        List<String> rows = new ArrayList<>(items.size());
        for (E item : items) {
            rows.add(mapper.apply(item));
        }
        return rows;
    }

    private static <E> List<E> parseAll(List<String> rows, Function<String, E> parser) {
        List<E> results = new ArrayList<>();
        for (String row : rows) {
            E parsed = parser.apply(row);
            if (parsed != null) {
                results.add(parsed);
            }
        }
        return results;
    }

    private static void ensureParentDirectory(String filePath) throws DataPersistenceException {
        try {
            Path parent = Paths.get(filePath).toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DataPersistenceException(filePath, "Could not create data directory", e);
        }
    }
}
