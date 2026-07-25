package com.airtribe.meditrack.constants;

/**
 * Application-wide constants for MediTrack.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>{@code static final} compile-time constants (inlined by the compiler).</li>
 *   <li>A <b>static initialisation block</b> that runs exactly once, when the class
 *       is initialised by the ClassLoader — used here to resolve the data directory
 *       from an environment variable with a sane fallback.</li>
 *   <li>A private constructor to prevent instantiation of a pure constants holder.</li>
 * </ul>
 *
 * @author Sunil (Utils, Storage, Singleton, Docs and Testing)
 */
public final class Constants {

    // ---------------------------------------------------------------- branding
    public static final String APP_NAME = "MediTrack";
    public static final String APP_VERSION = "1.0.0";
    public static final String ORG_NAME = "Airtribe Java Track";

    // ---------------------------------------------------------------- billing
    /** Goods and Services Tax applied to every bill (18%). */
    public static final double TAX_RATE = 0.18;

    /** Flat registration fee charged on a patient's first consultation. */
    public static final double REGISTRATION_FEE = 100.00;

    /** Surcharge multiplier applied to emergency bills. */
    public static final double EMERGENCY_SURCHARGE_RATE = 0.25;

    /** Discount applied for patients aged {@link #SENIOR_CITIZEN_AGE} and above. */
    public static final double SENIOR_CITIZEN_DISCOUNT_RATE = 0.10;

    /** Portion of a bill an insurance provider covers by default. */
    public static final double INSURANCE_COVERAGE_RATE = 0.70;

    public static final String CURRENCY_SYMBOL = "₹";

    // ---------------------------------------------------------------- domain rules
    public static final int MIN_AGE = 0;
    public static final int MAX_AGE = 120;
    public static final int SENIOR_CITIZEN_AGE = 60;
    public static final int CONTACT_NUMBER_LENGTH = 10;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;

    /** Length of one appointment slot, in minutes. */
    public static final int SLOT_DURATION_MINUTES = 30;

    /** Clinic opening hour, 24-hour clock. */
    public static final int CLINIC_OPEN_HOUR = 9;

    /** Clinic closing hour, 24-hour clock. */
    public static final int CLINIC_CLOSE_HOUR = 17;

    /** Maximum appointments a single doctor may hold on one calendar day. */
    public static final int MAX_APPOINTMENTS_PER_DOCTOR_PER_DAY = 8;

    // ---------------------------------------------------------------- id prefixes
    public static final String PATIENT_ID_PREFIX = "PAT";
    public static final String DOCTOR_ID_PREFIX = "DOC";
    public static final String APPOINTMENT_ID_PREFIX = "APT";
    public static final String BILL_ID_PREFIX = "BIL";

    /** Number of zero-padded digits in a generated id, e.g. {@code PAT-0001}. */
    public static final int ID_PADDING_WIDTH = 4;

    public static final String ID_SEPARATOR = "-";

    // ---------------------------------------------------------------- formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String DISPLAY_DATE_TIME_FORMAT = "dd MMM yyyy, hh:mm a";

    // ---------------------------------------------------------------- persistence
    public static final String CSV_DELIMITER = ",";
    public static final String CSV_ESCAPED_DELIMITER = "\\|\\|";
    public static final String LIST_DELIMITER = ";";

    /**
     * Root directory for all persisted data. Resolved once in the static block below.
     * Not {@code final} at declaration because its value is computed at class-init time.
     */
    public static final String DATA_DIR;

    public static final String PATIENTS_FILE;
    public static final String DOCTORS_FILE;
    public static final String APPOINTMENTS_FILE;
    public static final String BILLS_FILE;
    public static final String SERIALIZED_STATE_FILE;

    // ---------------------------------------------------------------- cli
    public static final String ARG_LOAD_DATA = "--loadData";
    public static final String ARG_SEED_DEMO = "--seedDemo";
    public static final String ARG_HELP = "--help";
    public static final String ARG_RUN_TESTS = "--runTests";

    /*
     * Static initialisation block.
     *
     * Runs once, on first active use of Constants, before any static member is read.
     * We use it to honour a MEDITRACK_DATA_DIR environment variable so the Docker
     * image can mount a volume elsewhere without recompiling.
     */
    static {
        String configured = System.getenv("MEDITRACK_DATA_DIR");
        if (configured == null || configured.isBlank()) {
            configured = "data";
        }
        DATA_DIR = configured;

        PATIENTS_FILE = DATA_DIR + "/patients.csv";
        DOCTORS_FILE = DATA_DIR + "/doctors.csv";
        APPOINTMENTS_FILE = DATA_DIR + "/appointments.csv";
        BILLS_FILE = DATA_DIR + "/bills.csv";
        SERIALIZED_STATE_FILE = DATA_DIR + "/meditrack.ser";

        System.out.println("[Constants] Static block executed — data directory resolved to: " + DATA_DIR);
    }

    /** Utility holder; never instantiated. */
    private Constants() {
        throw new AssertionError("Constants is a utility class and must not be instantiated.");
    }
}
