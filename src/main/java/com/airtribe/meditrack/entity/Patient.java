package main.java.com.airtribe.meditrack.entity;

public class Patient extends Person implements Cloneable {
    private String medicalHistory;

    public Patient(String id, String name, int age, String contactNumber, String medicalHistory) {
        super(id, name, age, contactNumber);
        this.medicalHistory = medicalHistory;
    }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

    @Override
    public void displayDetails() {
        System.out.println("Patient ID: " + getId() + ", Name: " + getName() + ", History: " + medicalHistory);
    }

    // Demonstrating Shallow Copy
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}