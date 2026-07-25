package com.airtribe.meditrack.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
