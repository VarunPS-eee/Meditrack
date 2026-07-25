package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;

public abstract class Person extends MedicalEntity {

    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private String contactNumber;
    private String email;

    protected Person(String id, String name, int age, String contactNumber) {
        this(id, name, age, contactNumber, null);
    }

    protected Person(String id, String name, int age, String contactNumber, String email) {
        super(id); // chain up to MedicalEntity
        this.name = name;
        this.age = age;
        this.contactNumber = contactNumber;
        this.email = email;
    }

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

    public String getMaskedContactNumber() {
        if (contactNumber == null || contactNumber.length() < 4) {
            return "N/A";
        }
        return "*".repeat(contactNumber.length() - 4)
                + contactNumber.substring(contactNumber.length() - 4);
    }

    @Override
    public String getSearchableText() {
        return String.join(" ",
                getId(),
                name == null ? "" : name,
                contactNumber == null ? "" : contactNumber,
                email == null ? "" : email);
    }
}
