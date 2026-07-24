package main.java.com.airtribe.meditrack.entity;

public class Doctor extends Person {
    private Specialization specialization; // Using the Enum we just created
    private double consultationFee;

    public Doctor(String id, String name, int age, String contactNumber, Specialization specialization, double consultationFee) {
        super(id, name, age, contactNumber); // Passes data to the Person superclass
        this.specialization = specialization;
        this.consultationFee = consultationFee;
    }

    public Specialization getSpecialization() { return specialization; }
    public void setSpecialization(Specialization specialization) { this.specialization = specialization; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    @Override
    public void displayDetails() {
        System.out.println("Doctor ID: " + getId() + ", Name: Dr. " + getName() + ", Spec: " + specialization);
    }
}