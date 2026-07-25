package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.exception.InvalidDataException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * The single place where input rules live.
 *
 * <p><b>Why centralise.</b> Without this class every setter, every service and every menu
 * handler would re-implement "is this name valid?", and they would drift. Encapsulation
 * is not just about {@code private} fields — it is about one owner per rule. Entities
 * stay dumb data holders; {@code Validator} owns correctness.</p>
 *
 * <p>Every method throws {@link InvalidDataException} naming the offending field, so the
 * console UI can tell the user exactly what to retype. The {@code isValidX} variants
 * return booleans for use in stream filters where an exception would be the wrong shape.</p>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class Validator {

    /** Letters, spaces, hyphens, apostrophes and dots — enough for real names. */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z .'-]*$");

    /** Indian mobile format: 10 digits starting 6–9. */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** {@code PAT-0001} and friends. */
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Z]{3}-\\d{4,}$");

    private static final Pattern BLOOD_GROUP_PATTERN = Pattern.compile("^(A|B|AB|O)[+-]$");

    private Validator() {
        throw new AssertionError("Validator is a utility class and must not be instantiated.");
    }

    // ------------------------------------------------------------------ strings
    /**
     * @param value     the value to check
     * @param fieldName the field being validated, used in the error message
     * @return the trimmed value
     * @throws InvalidDataException if null or blank
     */
    public static String requireNonBlank(String value, String fieldName) throws InvalidDataException {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(fieldName, value, "must not be empty");
        }
        return value.trim();
    }

    /**
     * Validates a person's name.
     *
     * @param name the name to validate
     * @return the trimmed name
     * @throws InvalidDataException if empty, too short, too long or containing digits
     */
    public static String validateName(String name) throws InvalidDataException {
        String trimmed = requireNonBlank(name, "name");
        if (trimmed.length() < Constants.MIN_NAME_LENGTH) {
            throw new InvalidDataException("name", name,
                    "must be at least " + Constants.MIN_NAME_LENGTH + " characters");
        }
        if (trimmed.length() > Constants.MAX_NAME_LENGTH) {
            throw new InvalidDataException("name", name,
                    "must be at most " + Constants.MAX_NAME_LENGTH + " characters");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException("name", name,
                    "may contain only letters, spaces, hyphens and apostrophes");
        }
        return trimmed;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isBlank()
                && name.trim().length() >= Constants.MIN_NAME_LENGTH
                && name.trim().length() <= Constants.MAX_NAME_LENGTH
                && NAME_PATTERN.matcher(name.trim()).matches();
    }

    // -------------------------------------------------------------------- age
    /**
     * Validates an age against the clinic's accepted range.
     *
     * @param age the age to validate
     * @return the same age
     * @throws InvalidDataException if outside {@code 0..120}
     */
    public static int validateAge(int age) throws InvalidDataException {
        if (age < Constants.MIN_AGE || age > Constants.MAX_AGE) {
            throw new InvalidDataException("age", age,
                    "must be between " + Constants.MIN_AGE + " and " + Constants.MAX_AGE);
        }
        return age;
    }

    /**
     * Parses and validates an age supplied as text.
     *
     * <p>Chains the underlying {@link NumberFormatException} rather than discarding it.</p>
     *
     * @param ageText the text to parse
     * @return the parsed, validated age
     * @throws InvalidDataException if unparseable or out of range
     */
    public static int validateAge(String ageText) throws InvalidDataException {
        String trimmed = requireNonBlank(ageText, "age");
        try {
            return validateAge(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            throw new InvalidDataException("age", ageText, "must be a whole number", e);
        }
    }

    public static boolean isValidAge(int age) {
        return age >= Constants.MIN_AGE && age <= Constants.MAX_AGE;
    }

    // ------------------------------------------------------------------ contact
    /**
     * Validates a 10-digit contact number, tolerating spaces, hyphens and a {@code +91} prefix.
     *
     * @param contactNumber the number to validate
     * @return the normalised 10-digit number
     * @throws InvalidDataException if it is not a valid mobile number
     */
    public static String validateContactNumber(String contactNumber) throws InvalidDataException {
        String trimmed = requireNonBlank(contactNumber, "contactNumber");
        String digits = trimmed.replaceAll("[\\s\\-()]", "").replaceFirst("^(\\+?91)", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw new InvalidDataException("contactNumber", contactNumber,
                    "must be a " + Constants.CONTACT_NUMBER_LENGTH + "-digit number starting with 6-9");
        }
        return digits;
    }

    public static boolean isValidContactNumber(String contactNumber) {
        if (contactNumber == null) {
            return false;
        }
        String digits = contactNumber.replaceAll("[\\s\\-()]", "").replaceFirst("^(\\+?91)", "");
        return PHONE_PATTERN.matcher(digits).matches();
    }

    /**
     * Validates an optional email address. {@code null} or blank is accepted, since email
     * is not mandatory — but a supplied value must be well-formed.
     *
     * @param email the address to validate, may be {@code null}
     * @return the trimmed address, or {@code null}
     * @throws InvalidDataException if a non-blank value is malformed
     */
    public static String validateEmail(String email) throws InvalidDataException {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException("email", email, "is not a valid email address");
        }
        return trimmed;
    }

    // ----------------------------------------------------------------- amounts
    /**
     * @param fee       the fee to validate
     * @param fieldName the field name for the error message
     * @return the same fee
     * @throws InvalidDataException if negative or implausibly large
     */
    public static double validateFee(double fee, String fieldName) throws InvalidDataException {
        if (fee < 0) {
            throw new InvalidDataException(fieldName, fee, "must not be negative");
        }
        if (fee > 1_000_000) {
            throw new InvalidDataException(fieldName, fee, "exceeds the maximum allowed amount");
        }
        return fee;
    }

    /**
     * Parses and validates a monetary amount supplied as text.
     *
     * @param feeText   the text to parse
     * @param fieldName the field name for the error message
     * @return the parsed, validated amount
     * @throws InvalidDataException if unparseable or out of range
     */
    public static double validateFee(String feeText, String fieldName) throws InvalidDataException {
        String trimmed = requireNonBlank(feeText, fieldName);
        try {
            return validateFee(Double.parseDouble(trimmed), fieldName);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(fieldName, feeText, "must be a number", e);
        }
    }

    // --------------------------------------------------------------------- ids
    /**
     * @param id        the id to validate
     * @param fieldName the field name for the error message
     * @return the trimmed, upper-cased id
     * @throws InvalidDataException if it does not match {@code XXX-9999}
     */
    public static String validateId(String id, String fieldName) throws InvalidDataException {
        String trimmed = requireNonBlank(id, fieldName).toUpperCase();
        if (!ID_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidDataException(fieldName, id, "must look like PAT-0001");
        }
        return trimmed;
    }

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id.trim().toUpperCase()).matches();
    }

    // ------------------------------------------------------------------- misc
    /**
     * @param bloodGroup the group to validate, may be {@code null}
     * @return the normalised group, or {@code null}
     * @throws InvalidDataException if a non-blank value is not a real blood group
     */
    public static String validateBloodGroup(String bloodGroup) throws InvalidDataException {
        if (bloodGroup == null || bloodGroup.isBlank()) {
            return null;
        }
        String normalised = bloodGroup.trim().toUpperCase();
        if (!BLOOD_GROUP_PATTERN.matcher(normalised).matches()) {
            throw new InvalidDataException("bloodGroup", bloodGroup,
                    "must be one of A+, A-, B+, B-, AB+, AB-, O+, O-");
        }
        return normalised;
    }

    /**
     * Validates a proposed appointment slot against clinic rules: it must exist, be in
     * the future, and fall within opening hours.
     *
     * @param slot the slot to validate
     * @return the same slot
     * @throws InvalidDataException if any rule is broken
     */
    public static LocalDateTime validateAppointmentSlot(LocalDateTime slot) throws InvalidDataException {
        if (slot == null) {
            throw new InvalidDataException("slot", null, "an appointment date/time is required");
        }
        if (!DateUtil.isFuture(slot)) {
            throw new InvalidDataException("slot", DateUtil.format(slot),
                    "appointments must be booked in the future");
        }
        if (!DateUtil.isWithinClinicHours(slot)) {
            throw new InvalidDataException("slot", DateUtil.format(slot),
                    String.format("clinic hours are %02d:00-%02d:00",
                            Constants.CLINIC_OPEN_HOUR, Constants.CLINIC_CLOSE_HOUR));
        }
        return slot;
    }

    /**
     * Guards against a {@code null} object reference.
     *
     * @param value     the reference to check
     * @param fieldName the field name for the error message
     * @param <T>       the referenced type
     * @return the same reference
     * @throws InvalidDataException if {@code null}
     */
    public static <T> T requireNonNull(T value, String fieldName) throws InvalidDataException {
        if (value == null) {
            throw new InvalidDataException(fieldName, null, "is required");
        }
        return value;
    }
}
