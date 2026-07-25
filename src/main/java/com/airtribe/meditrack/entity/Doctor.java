package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.interfaces.Payable;

import java.util.Comparator;

public class Doctor extends Person {

    private static final long serialVersionUID = 1L;

    /** Cheapest first. */
    public static final Comparator<Doctor> BY_FEE =
            Comparator.comparingDouble(Doctor::getConsultationFee);

    /** Alphabetical, case-insensitive. */
    public static final Comparator<Doctor> BY_NAME =
            Comparator.comparing(Doctor::getName, String.CASE_INSENSITIVE_ORDER);

    /** Most experienced first. */
    public static final Comparator<Doctor> BY_EXPERIENCE =
            Comparator.comparingInt(Doctor::getYearsOfExperience).reversed();

    /** Highest rated first, ties broken by experience then name — a stable total order. */
    public static final Comparator<Doctor> BY_RATING =
            Comparator.comparingDouble(Doctor::getRating).reversed()
                    .thenComparing(BY_EXPERIENCE)
                    .thenComparing(BY_NAME);

    private Specialization specialization;
    private double consultationFee;
    private int yearsOfExperience;
    private double rating;
    private boolean available;

    public Doctor(String id, String name, int age, String contactNumber, Specialization specialization) {
        this(id, name, age, contactNumber, specialization,
                specialization == null ? 0.0 : specialization.getBaseConsultationFee(), 0, 0.0);
    }

    /** Constructor matching the original 6-arg signature, kept for source compatibility. */
    public Doctor(String id, String name, int age, String contactNumber,
                  Specialization specialization, double consultationFee) {
        this(id, name, age, contactNumber, specialization, consultationFee, 0, 0.0);
    }

    public Doctor(String id, String name, int age, String contactNumber,
                  Specialization specialization, double consultationFee,
                  int yearsOfExperience, double rating) {
        super(id, name, age, contactNumber);
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.yearsOfExperience = yearsOfExperience;
        this.rating = rating;
        this.available = true;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
        touch();
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
        touch();
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
        touch();
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
        touch();
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
        touch();
    }

    public String getSeniorityBand() {
        if (yearsOfExperience < 3) {
            return "JUNIOR";
        } else if (yearsOfExperience < 8) {
            return "MID";
        } else if (yearsOfExperience < 15) {
            return "SENIOR";
        }
        return "CONSULTANT";
    }

    @Override
    public String getEntityType() {
        return "Doctor";
    }

    /** Overridden to make the speciality searchable alongside the inherited fields. */
    @Override
    public String getSearchableText() {
        return super.getSearchableText() + " "
                + (specialization == null ? "" : specialization.name() + " " + specialization.getDisplayName())
                + " " + getSeniorityBand();
    }

    @Override
    public void displayDetails() {
        StringBuilder sb = new StringBuilder(160);
        sb.append("  [").append(getId()).append("] Dr. ").append(getName())
                .append("  |  ").append(specialization == null ? "Unassigned" : specialization.getDisplayName())
                .append("  |  Fee: ").append(Payable.formatCurrency(consultationFee))
                .append("  |  Exp: ").append(yearsOfExperience).append("y (").append(getSeniorityBand()).append(')')
                .append("  |  Rating: ").append(String.format("%.1f", rating)).append("/5.0")
                .append("  |  ").append(available ? "AVAILABLE" : "UNAVAILABLE");
        System.out.println(sb);
    }

    @Override
    public String toString() {
        return "Doctor{id='" + getId() + "', name='" + getName()
                + "', specialization=" + specialization + ", fee=" + consultationFee + '}';
    }
}
