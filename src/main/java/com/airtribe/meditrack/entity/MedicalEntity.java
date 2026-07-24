package main.java.com.airtribe.meditrack.entity;

public abstract class MedicalEntity {
    private String id;

    public MedicalEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    // Abstract method to enforce behavior in subclasses
    public abstract void displayDetails();
}