package com.airtribe.meditrack.exception;

import java.time.LocalDateTime;

/**
 * Thrown when a requested appointment slot cannot be booked — the doctor is already
 * occupied, the slot falls outside clinic hours, or the daily cap is reached.
 *
 * @author Zubair (Services, Logic, Observer and AI)
 */
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
