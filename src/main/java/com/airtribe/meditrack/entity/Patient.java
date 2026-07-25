package com.airtribe.meditrack.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Someone receiving care at the clinic.
 *
 * <p>This class is the project's <b>deep vs shallow copy</b> demonstration. It holds a
 * mutable {@code List<String>} of medical history, which is exactly the situation where
 * {@link Object#clone()}'s default behaviour goes wrong: the default copies the
 * <em>reference</em> to the list, so the "copy" and the original share one list and
 * edits to either are visible in both.</p>
 *
 * <p>Two copy methods are provided side by side so the difference can be observed
 * rather than asserted:</p>
 * <ul>
 *   <li>{@link #shallowCopy()} — {@code super.clone()} only. Shares the history list.</li>
 *   <li>{@link #clone()} — copies the lists too. Fully independent.</li>
 * </ul>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public class Patient extends Person implements Cloneable {

    private static final long serialVersionUID = 1L;

    /** Mutable reference type — the reason a shallow copy is not enough. */
    private List<String> medicalHistory;

    /** A second mutable collection, to show the problem is not specific to one field. */
    private List<String> allergies;

    private String bloodGroup;
    private boolean insured;

    /** Constructor matching the original signature; history is seeded from one string. */
    public Patient(String id, String name, int age, String contactNumber, String medicalHistory) {
        this(id, name, age, contactNumber,
                medicalHistory == null || medicalHistory.isBlank()
                        ? new ArrayList<>()
                        : new ArrayList<>(List.of(medicalHistory)),
                new ArrayList<>(), null, false);
    }

    /**
     * Canonical constructor.
     *
     * @param id             business key
     * @param name           full name
     * @param age            age in years
     * @param contactNumber  contact number
     * @param medicalHistory prior conditions and notes
     * @param allergies      known allergies
     * @param bloodGroup     blood group, e.g. {@code O+}
     * @param insured        whether the patient carries insurance
     */
    public Patient(String id, String name, int age, String contactNumber,
                   List<String> medicalHistory, List<String> allergies,
                   String bloodGroup, boolean insured) {
        super(id, name, age, contactNumber);
        // Defensive copy on the way in: the caller's list must not become our state.
        this.medicalHistory = medicalHistory == null ? new ArrayList<>() : new ArrayList<>(medicalHistory);
        this.allergies = allergies == null ? new ArrayList<>() : new ArrayList<>(allergies);
        this.bloodGroup = bloodGroup;
        this.insured = insured;
    }

    // --------------------------------------------------------------- accessors
    /** @return an unmodifiable view — callers can read the history but not corrupt it */
    public List<String> getMedicalHistory() {
        return Collections.unmodifiableList(medicalHistory);
    }

    public void setMedicalHistory(List<String> medicalHistory) {
        this.medicalHistory = medicalHistory == null ? new ArrayList<>() : new ArrayList<>(medicalHistory);
        touch();
    }

    public void addMedicalHistoryEntry(String entry) {
        if (entry != null && !entry.isBlank()) {
            this.medicalHistory.add(entry.trim());
            touch();
        }
    }

    public List<String> getAllergies() {
        return Collections.unmodifiableList(allergies);
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies == null ? new ArrayList<>() : new ArrayList<>(allergies);
        touch();
    }

    public void addAllergy(String allergy) {
        if (allergy != null && !allergy.isBlank()) {
            this.allergies.add(allergy.trim());
            touch();
        }
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
        touch();
    }

    public boolean isInsured() {
        return insured;
    }

    public void setInsured(boolean insured) {
        this.insured = insured;
        touch();
    }

    // ------------------------------------------------------------ copy semantics
    /**
     * <b>Deep copy.</b> Clones the object, then replaces every mutable reference field
     * with a fresh copy of its own. The result shares no mutable state with the original.
     *
     * <p>Immutable fields ({@code String}, {@code int}, {@code boolean}) need no special
     * handling — that is the whole point of immutability.</p>
     *
     * @return a fully independent copy
     * @throws CloneNotSupportedException never, in practice — declared to honour the contract
     */
    @Override
    public Patient clone() throws CloneNotSupportedException {
        Patient copy = (Patient) super.clone();   // step 1: bitwise field copy
        copy.medicalHistory = new ArrayList<>(this.medicalHistory); // step 2: new list
        copy.allergies = new ArrayList<>(this.allergies);           // step 2: new list
        return copy;
    }

    /**
     * <b>Shallow copy.</b> Deliberately stops after {@code super.clone()}, so the returned
     * patient shares the <em>same</em> history and allergy lists as this one.
     *
     * <p>Kept in the codebase purely to make the failure mode demonstrable in
     * {@code TestRunner}: mutate the original's history and the shallow copy changes too,
     * while the deep copy does not.</p>
     *
     * @return a copy that shares this patient's mutable collections
     * @throws CloneNotSupportedException never, in practice
     */
    public Patient shallowCopy() throws CloneNotSupportedException {
        return (Patient) super.clone();
    }

    // --------------------------------------------------------------- behaviour
    @Override
    public String getEntityType() {
        return "Patient";
    }

    @Override
    public String getSearchableText() {
        return super.getSearchableText() + " "
                + String.join(" ", medicalHistory) + " "
                + String.join(" ", allergies) + " "
                + (bloodGroup == null ? "" : bloodGroup);
    }

    /** Overridden: prints a patient's shape, not a doctor's. Dynamic dispatch in action. */
    @Override
    public void displayDetails() {
        StringBuilder sb = new StringBuilder(180);
        sb.append("  [").append(getId()).append("] ").append(getName())
                .append("  |  Age: ").append(getAge()).append(" (").append(getAgeGroup()).append(')')
                .append("  |  Blood: ").append(bloodGroup == null ? "N/A" : bloodGroup)
                .append("  |  Contact: ").append(getMaskedContactNumber())
                .append("  |  Insured: ").append(insured ? "YES" : "NO");
        if (!medicalHistory.isEmpty()) {
            sb.append("\n        History  : ").append(String.join(", ", medicalHistory));
        }
        if (!allergies.isEmpty()) {
            sb.append("\n        Allergies: ").append(String.join(", ", allergies));
        }
        System.out.println(sb);
    }

    @Override
    public String toString() {
        return "Patient{id='" + getId() + "', name='" + getName()
                + "', age=" + getAge() + ", history=" + medicalHistory + '}';
    }
}
