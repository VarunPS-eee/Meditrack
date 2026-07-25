package com.airtribe.meditrack.exception;

import java.time.LocalDateTime;

public class SlotUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final LocalDateTime requestedSlot;

    public SlotUnavailableException(String doctorId, LocalDateTime requestedSlot, String reason) {
        super(String.format("Slot %s unavailable for doctor %s — %s", requestedSlot, doctorId, reason));
        this.doctorId = doctorId;
        this.requestedSlot = requestedSlot;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public LocalDateTime getRequestedSlot() {
        return requestedSlot;
    }
}
