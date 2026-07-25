package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Medical specialities a doctor can hold.
 *
 * <p>Demonstrates that a Java enum is a full class: it carries fields, a constructor,
 * instance methods and static helpers. Using this instead of {@code String} makes
 * invalid specialities <em>unrepresentable</em> rather than merely discouraged.</p>
 *
 * <p>Each constant also carries the symptom keywords used by
 * {@code AIHelper} for rule-based doctor recommendation, which keeps the mapping
 * next to the speciality it describes rather than in a far-away lookup table.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public enum Specialization {

    CARDIOLOGY("Cardiology", 1200.0,
            "chest pain", "heart", "palpitation", "breathless", "bp", "blood pressure", "cholesterol"),

    DERMATOLOGY("Dermatology", 800.0,
            "skin", "rash", "acne", "itching", "hair fall", "eczema", "allergy"),

    NEUROLOGY("Neurology", 1500.0,
            "headache", "migraine", "seizure", "numbness", "dizziness", "memory", "tremor"),

    ORTHOPEDICS("Orthopedics", 1000.0,
            "bone", "fracture", "joint", "back pain", "knee", "shoulder", "sprain"),

    PEDIATRICS("Pediatrics", 700.0,
            "child", "infant", "vaccination", "growth", "baby", "toddler"),

    GENERAL_PRACTICE("General Practice", 500.0,
            "fever", "cold", "cough", "fatigue", "checkup", "general", "flu", "body ache");

    /** Human-friendly label for console output. */
    private final String displayName;

    /** Typical consultation fee for this speciality, used as a seed default. */
    private final double baseConsultationFee;

    /** Symptom keywords that route a patient to this speciality. */
    private final List<String> symptomKeywords;

    Specialization(String displayName, double baseConsultationFee, String... symptomKeywords) {
        this.displayName = displayName;
        this.baseConsultationFee = baseConsultationFee;
        this.symptomKeywords = Collections.unmodifiableList(Arrays.asList(symptomKeywords));
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseConsultationFee() {
        return baseConsultationFee;
    }

    /** @return an unmodifiable view of this speciality's symptom keywords */
    public List<String> getSymptomKeywords() {
        return symptomKeywords;
    }

    /**
     * Scores how well a free-text symptom description matches this speciality.
     * Used by the AI helper to rank recommendations.
     *
     * @param symptomText the patient's described symptoms
     * @return number of keywords found in the text; {@code 0} means no match
     */
    public int scoreAgainst(String symptomText) {
        if (symptomText == null || symptomText.isBlank()) {
            return 0;
        }
        String haystack = symptomText.toLowerCase();
        int score = 0;
        for (String keyword : symptomKeywords) {
            if (haystack.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    /**
     * Null-safe, case-insensitive lookup that also accepts the display name.
     *
     * <p>Returns an {@link Optional} rather than throwing, so callers decide how a
     * bad speciality string should be handled.</p>
     *
     * @param value user input such as {@code "cardiology"} or {@code "General Practice"}
     * @return the matching constant, if any
     */
    public static Optional<Specialization> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalised = value.trim().replace(' ', '_').toUpperCase();
        return Arrays.stream(values())
                .filter(s -> s.name().equals(normalised) || s.displayName.equalsIgnoreCase(value.trim()))
                .findFirst();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
