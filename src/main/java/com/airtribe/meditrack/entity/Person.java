package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;

/**
 * A human being known to the clinic — the shared parent of {@link Doctor} and {@link Patient}.
 *
 * <p><b>Inheritance.</b> {@code Person} sits between {@link MedicalEntity} and the
 * concrete roles, holding what every person has (name, age, contact) and nothing about
 * what they <em>do</em>. It stays {@code abstract} because a bare "person" is not a
 * thing the clinic can act on, and it deliberately does not implement
 * {@link MedicalEntity#displayDetails()} — that stays the subclass's job.</p>
 *
 * <p><b>Constructor chaining.</b> The 4-arg constructor delegates to the 5-arg one with
 * {@code this(...)}, which in turn calls {@code super(id)}. One initialisation path,
 * no duplicated field assignment.</p>
 *
 * @author Varun (Core Entities, OOP and Factory)
 */
public abstract class Person extends MedicalEntity {

    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private String contactNumber;
    private String email;

    /**
     * Convenience constructor — delegates rather than duplicating.
     *
     * @param id            business key
     * @param name          full name
     * @param age           age in years
     * @param contactNumber 10-digit contact number
     */
    protected Person(String id, String name, int age, String contactNumber) {
        this(id, name, age, contactNumber, null);
    }

    /**
     * Canonical constructor — the single place where a {@code Person}'s fields are set.
     *
     * @param id            business key
     * @param name          full name
     * @param age           age in years
     * @param contactNumber 10-digit contact number
     * @param email         optional email address
     */
    protected Person(String id, String name, int age, String contactNumber, String email) {
        super(id); // chain up to MedicalEntity
        this.name = name;
        this.age = age;
        this.contactNumber = contactNumber;
        this.email = email;
    }

    // ---------------------------------------------------------------- encapsulation
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
        touch();
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        touch();
    }

    // ------------------------------------------------------------------- behaviour
    /**
     * @return {@code true} if this person qualifies for the senior-citizen concession
     */
    public boolean isSeniorCitizen() {
        return age >= Constants.SENIOR_CITIZEN_AGE;
    }

    /**
     * @return the person's age bracket, used in reporting
     */
    public String getAgeGroup() {
        if (age < 13) {
            return "CHILD";
        } else if (age < 20) {
            return "TEEN";
        } else if (age < Constants.SENIOR_CITIZEN_AGE) {
            return "ADULT";
        }
        return "SENIOR";
    }

    /**
     * Masks all but the last four digits before printing, so a console screenshot
     * pasted into a PR does not leak a real phone number.
     *
     * @return e.g. {@code ******3210}
     */
    public String getMaskedContactNumber() {
        if (contactNumber == null || contactNumber.length() < 4) {
            return "N/A";
        }
        return "*".repeat(contactNumber.length() - 4)
                + contactNumber.substring(contactNumber.length() - 4);
    }

    /**
     * Base searchable text every person contributes. Subclasses override and call
     * {@code super.getSearchableText()} to add their own fields.
     */
    @Override
    public String getSearchableText() {
        return String.join(" ",
                getId(),
                name == null ? "" : name,
                contactNumber == null ? "" : contactNumber,
                email == null ? "" : email);
    }
}
