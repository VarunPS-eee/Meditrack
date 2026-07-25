package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rule-based clinical triage: recommends a speciality from described symptoms, ranks
 * doctors, and suggests appointment slots.
 *
 * <h2>What "AI" means here</h2>
 * <p>This is a deterministic <b>expert system</b>, not a learned model — no training data,
 * no network calls, no dependencies. Scores come from keyword matches declared on
 * {@link Specialization}, so every recommendation can be explained by pointing at the
 * keywords that fired. For a clinical triage aid that transparency is a feature, not a
 * limitation.</p>
 *
 * <h2>Ranking</h2>
 * <p>Doctors are scored on a weighted blend of speciality match, rating, experience and
 * current load, so the system does not simply funnel every patient to one popular
 * consultant.</p>
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
public final class AIHelper {

    /** Weight applied to how well the doctor's speciality matches the symptoms. */
    private static final double WEIGHT_SPECIALITY = 10.0;

    /** Weight applied to the doctor's patient rating. */
    private static final double WEIGHT_RATING = 2.0;

    /** Weight applied to years of experience (capped, so seniority does not dominate). */
    private static final double WEIGHT_EXPERIENCE = 0.3;

    /** Penalty per existing appointment, to spread load across the roster. */
    private static final double PENALTY_PER_BOOKING = 0.8;

    /** Urgency keywords that route a patient straight to emergency triage. */
    private static final List<String> URGENT_KEYWORDS = List.of(
            "severe", "emergency", "unconscious", "bleeding", "chest pain",
            "breathless", "accident", "collapse", "poison", "stroke");

    private AIHelper() {
        throw new AssertionError("AIHelper is a utility class and must not be instantiated.");
    }

    /**
     * A scored speciality recommendation.
     *
     * @param specialization the recommended speciality
     * @param score          how many symptom keywords matched
     * @param matchedKeywords the keywords responsible for the score
     */
    public record SpecialityMatch(Specialization specialization, int score, List<String> matchedKeywords) {

        /** @return confidence as a percentage of the best achievable score */
        public double confidencePercent(int bestScore) {
            return bestScore == 0 ? 0.0 : (score * 100.0) / bestScore;
        }
    }

    /**
     * A scored doctor recommendation.
     *
     * @param doctor the recommended doctor
     * @param score  the weighted suitability score
     * @param reason a human-readable justification
     */
    public record DoctorRecommendation(Doctor doctor, double score, String reason) {
    }

    // ------------------------------------------------------- speciality triage
    /**
     * Ranks every speciality against the described symptoms.
     *
     * @param symptomText free-text symptom description
     * @return matching specialities, best first; empty if nothing matched
     */
    public static List<SpecialityMatch> recommendSpecialities(String symptomText) {
        if (symptomText == null || symptomText.isBlank()) {
            return List.of();
        }
        String haystack = symptomText.toLowerCase();

        return java.util.Arrays.stream(Specialization.values())
                .map(spec -> {
                    List<String> matched = spec.getSymptomKeywords().stream()
                            .filter(haystack::contains)
                            .collect(Collectors.toList());
                    return new SpecialityMatch(spec, matched.size(), matched);
                })
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(SpecialityMatch::score).reversed())
                .collect(Collectors.toList());
    }

    /**
     * The single best speciality for the described symptoms.
     *
     * <p>Falls back to {@link Specialization#GENERAL_PRACTICE} when nothing matches —
     * a triage system that refuses to answer is worse than one that routes to a
     * generalist.</p>
     *
     * @param symptomText free-text symptom description
     * @return the recommended speciality
     */
    public static Specialization recommendSpecialization(String symptomText) {
        return recommendSpecialities(symptomText).stream()
                .findFirst()
                .map(SpecialityMatch::specialization)
                .orElse(Specialization.GENERAL_PRACTICE);
    }

    /**
     * Flags symptom descriptions that warrant emergency handling.
     *
     * @param symptomText free-text symptom description
     * @return {@code true} if any urgency keyword is present
     */
    public static boolean isUrgent(String symptomText) {
        if (symptomText == null || symptomText.isBlank()) {
            return false;
        }
        String haystack = symptomText.toLowerCase();
        return URGENT_KEYWORDS.stream().anyMatch(haystack::contains);
    }

    // --------------------------------------------------------- doctor ranking
    /**
     * Recommends doctors for the described symptoms, best match first.
     *
     * @param symptomText  free-text symptom description
     * @param doctors      the roster to choose from
     * @param appointments existing appointments, used to spread load
     * @param limit        how many recommendations to return
     * @return the ranked recommendations
     */
    public static List<DoctorRecommendation> recommendDoctors(String symptomText,
                                                              List<Doctor> doctors,
                                                              List<Appointment> appointments,
                                                              int limit) {
        if (doctors == null || doctors.isEmpty()) {
            return List.of();
        }
        Specialization target = recommendSpecialization(symptomText);
        Map<String, Long> loadByDoctor = countLoad(appointments);

        return doctors.stream()
                .filter(Doctor::isAvailable)
                .map(doctor -> scoreDoctor(doctor, target, loadByDoctor))
                .sorted(Comparator.comparingDouble(DoctorRecommendation::score).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /** Scores one doctor against a target speciality and the current booking load. */
    private static DoctorRecommendation scoreDoctor(Doctor doctor,
                                                    Specialization target,
                                                    Map<String, Long> loadByDoctor) {
        double score = 0.0;
        StringBuilder reason = new StringBuilder(120);

        boolean specialityMatches = doctor.getSpecialization() == target;
        if (specialityMatches) {
            score += WEIGHT_SPECIALITY;
            reason.append("specialises in ").append(target.getDisplayName());
        } else {
            reason.append(doctor.getSpecialization() == null
                    ? "no speciality recorded"
                    : "adjacent speciality (" + doctor.getSpecialization().getDisplayName() + ")");
        }

        score += doctor.getRating() * WEIGHT_RATING;
        // Experience is capped at 20 years so a 40-year veteran does not outrank
        // a well-matched specialist purely on longevity.
        score += Math.min(doctor.getYearsOfExperience(), 20) * WEIGHT_EXPERIENCE;

        long load = loadByDoctor.getOrDefault(doctor.getId(), 0L);
        score -= load * PENALTY_PER_BOOKING;

        reason.append(String.format("; rating %.1f, %dy experience, %d booked",
                doctor.getRating(), doctor.getYearsOfExperience(), load));

        return new DoctorRecommendation(doctor, score, reason.toString());
    }

    private static Map<String, Long> countLoad(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Map.of();
        }
        return appointments.stream()
                .filter(a -> a.getDoctor() != null && !a.getStatus().isTerminal())
                .collect(Collectors.groupingBy(a -> a.getDoctor().getId(), Collectors.counting()));
    }

    // ----------------------------------------------------------- slot suggestion
    /**
     * Suggests the next free slots for a doctor, searching forward day by day.
     *
     * @param doctor         the doctor to book with
     * @param appointments   existing appointments
     * @param maxSuggestions how many slots to return
     * @param daysToSearch   how many days ahead to look
     * @return the suggested slots, soonest first
     */
    public static List<LocalDateTime> suggestSlots(Doctor doctor,
                                                   List<Appointment> appointments,
                                                   int maxSuggestions,
                                                   int daysToSearch) {
        List<LocalDateTime> suggestions = new ArrayList<>();
        if (doctor == null) {
            return suggestions;
        }

        List<LocalDateTime> booked = appointments == null ? List.of() : appointments.stream()
                .filter(a -> a.getDoctor() != null && a.getDoctor().getId().equals(doctor.getId()))
                .filter(a -> !a.getStatus().isTerminal())
                .map(a -> DateUtil.alignToSlot(a.getSlot()))
                .toList();

        LocalDate day = LocalDate.now();
        for (int i = 0; i < Math.max(1, daysToSearch) && suggestions.size() < maxSuggestions; i++) {
            LocalDate candidate = day.plusDays(i);
            for (LocalDateTime slot : DateUtil.generateSlotsForDay(candidate)) {
                if (suggestions.size() >= maxSuggestions) {
                    break;
                }
                if (DateUtil.isFuture(slot) && !booked.contains(slot)) {
                    suggestions.add(slot);
                }
            }
        }
        return suggestions;
    }

    /**
     * Suggests the earliest slot across every doctor able to treat the symptoms.
     *
     * @param symptomText  free-text symptom description
     * @param doctors      the roster
     * @param appointments existing appointments
     * @return the doctor and slot to offer, if one exists
     */
    public static Optional<Map.Entry<Doctor, LocalDateTime>> suggestEarliestAppointment(
            String symptomText, List<Doctor> doctors, List<Appointment> appointments) {

        List<DoctorRecommendation> ranked = recommendDoctors(symptomText, doctors, appointments, 5);
        for (DoctorRecommendation recommendation : ranked) {
            List<LocalDateTime> slots = suggestSlots(recommendation.doctor(), appointments, 1, 7);
            if (!slots.isEmpty()) {
                return Optional.of(Map.entry(recommendation.doctor(), slots.get(0)));
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------- explanation
    /**
     * Builds a printable triage report explaining the recommendation.
     *
     * @param symptomText  the described symptoms
     * @param doctors      the roster
     * @param appointments existing appointments
     * @return the report text
     */
    public static String buildTriageReport(String symptomText,
                                           List<Doctor> doctors,
                                           List<Appointment> appointments) {
        StringBuilder sb = new StringBuilder(600);
        sb.append("\n  ").append("=".repeat(72)).append('\n')
                .append("   AI TRIAGE — rule-based recommendation\n")
                .append("  ").append("=".repeat(72)).append('\n')
                .append("   Symptoms: ").append(symptomText).append('\n');

        if (isUrgent(symptomText)) {
            sb.append("\n   ** URGENT ** These symptoms suggest emergency care. ")
                    .append("Escalate before booking a routine slot.\n");
        }

        List<SpecialityMatch> matches = recommendSpecialities(symptomText);
        sb.append("\n   Speciality match\n   ").append("-".repeat(68)).append('\n');
        if (matches.isEmpty()) {
            sb.append("   No keyword matched — defaulting to General Practice.\n");
        } else {
            int best = matches.get(0).score();
            for (SpecialityMatch match : matches) {
                sb.append(String.format("   %-18s %3d pts (%.0f%% confidence)  matched: %s%n",
                        match.specialization().getDisplayName(),
                        match.score(),
                        match.confidencePercent(best),
                        String.join(", ", match.matchedKeywords())));
            }
        }

        List<DoctorRecommendation> recommendations =
                recommendDoctors(symptomText, doctors, appointments, 3);
        sb.append("\n   Recommended doctors\n   ").append("-".repeat(68)).append('\n');
        if (recommendations.isEmpty()) {
            sb.append("   No available doctor could be matched.\n");
        } else {
            int rank = 1;
            for (DoctorRecommendation recommendation : recommendations) {
                sb.append(String.format("   %d. Dr. %-20s score %.1f%n      %s%n",
                        rank++,
                        recommendation.doctor().getName(),
                        recommendation.score(),
                        recommendation.reason()));
            }
        }

        suggestEarliestAppointment(symptomText, doctors, appointments).ifPresent(entry ->
                sb.append("\n   Earliest slot: Dr. ").append(entry.getKey().getName())
                        .append(" at ").append(DateUtil.formatForDisplay(entry.getValue())).append('\n'));

        sb.append("  ").append("=".repeat(72));
        return sb.toString();
    }

    /**
     * Suggests history notes worth capturing, based on the patient's recorded allergies.
     *
     * @param patient the patient
     * @return prompts for the clinician, empty if nothing to flag
     */
    public static List<String> suggestScreeningPrompts(Patient patient) {
        Map<String, String> prompts = new LinkedHashMap<>();
        if (patient == null) {
            return List.of();
        }
        if (patient.getAllergies().isEmpty()) {
            prompts.put("allergy", "No allergies on file — confirm before prescribing.");
        }
        if (patient.getBloodGroup() == null) {
            prompts.put("blood", "Blood group not recorded — capture at this visit.");
        }
        if (patient.isSeniorCitizen()) {
            prompts.put("senior", "Senior patient — consider a routine BP and vitals check.");
        }
        if (patient.getMedicalHistory().isEmpty()) {
            prompts.put("history", "First visit — take a full medical history.");
        }
        return new ArrayList<>(prompts.values());
    }
}
