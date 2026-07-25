package com.airtribe.meditrack.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Override
    public Patient clone() throws CloneNotSupportedException {
        Patient copy = (Patient) super.clone();   // step 1: bitwise field copy
        copy.medicalHistory = new ArrayList<>(this.medicalHistory); // step 2: new list
        copy.allergies = new ArrayList<>(this.allergies);           // step 2: new list
        return copy;
    }

    public Patient shallowCopy() throws CloneNotSupportedException {
        return (Patient) super.clone();
    }

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
