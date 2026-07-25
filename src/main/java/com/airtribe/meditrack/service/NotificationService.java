package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.interfaces.AppointmentObserver;
import com.airtribe.meditrack.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class NotificationService {

    /** Event names dispatched to observers. */
    public static final String EVENT_BOOKED = "BOOKED";
    public static final String EVENT_CANCELLED = "CANCELLED";
    public static final String EVENT_RESCHEDULED = "RESCHEDULED";
    public static final String EVENT_COMPLETED = "COMPLETED";
    public static final String EVENT_REMINDER = "REMINDER";

    /** Appointments within this many hours are reminded about. */
    private static final long REMINDER_WINDOW_HOURS = 24;

    private final List<AppointmentObserver> observers = new CopyOnWriteArrayList<>();

    /** Atomic because the main thread and the timer thread both increment it. */
    private final AtomicInteger eventsDispatched = new AtomicInteger(0);

    /** Guards the reminder sweep against overlapping runs. */
    private final Object sweepLock = new Object();

    private Timer reminderTimer;
    private boolean enabled = true;

    /**
     * @param observer the channel to add; {@code null} and duplicates are ignored
     */
    public void register(AppointmentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public boolean unregister(AppointmentObserver observer) {
        return observers.remove(observer);
    }

    public int getObserverCount() {
        return observers.size();
    }

    public List<AppointmentObserver> getObservers() {
        return List.copyOf(observers);
    }

    public int getEventsDispatched() {
        return eventsDispatched.get();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void notifyObservers(Appointment appointment, String eventType) {
        if (!enabled || appointment == null) {
            return;
        }
        eventsDispatched.incrementAndGet();

        for (AppointmentObserver observer : observers) {
            if (!observer.isInterestedIn(eventType)) {
                continue;
            }
            try {
                observer.update(appointment, eventType);
            } catch (RuntimeException e) {
                System.out.printf("    [WARN] Observer %s failed: %s%n",
                        observer.getObserverName(), e.getMessage());
            }
        }
    }

    private class ReminderTask extends TimerTask {

        private final Supplier<List<Appointment>> appointmentSupplier;

        ReminderTask(Supplier<List<Appointment>> appointmentSupplier) {
            this.appointmentSupplier = appointmentSupplier;
        }

        @Override
        public void run() {
            // synchronized: if a sweep runs long, the next tick waits rather than
            // interleaving and double-reminding the same patient.
            synchronized (sweepLock) {
                try {
                    sweepOnce(appointmentSupplier.get());
                } catch (RuntimeException e) {
                    System.out.println("    [WARN] Reminder sweep failed: " + e.getMessage());
                }
            }
        }
    }

    public int sweepOnce(List<Appointment> appointments) {
        if (appointments == null || !enabled) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        for (Appointment appointment : appointments) {
            if (!appointment.isUpcoming()) {
                continue;
            }
            long hoursAway = DateUtil.hoursBetween(now, appointment.getSlot());
            if (hoursAway >= 0 && hoursAway <= REMINDER_WINDOW_HOURS) {
                notifyObservers(appointment, EVENT_REMINDER);
                sent++;
            }
        }
        return sent;
    }

    public void startReminderScheduler(Supplier<List<Appointment>> appointmentSupplier, long periodSeconds) {
        if (reminderTimer != null) {
            return;   // already running
        }
        // daemon = true: this thread must not keep the JVM alive after the user quits.
        reminderTimer = new Timer("meditrack-reminder", true);
        long periodMillis = Math.max(1, periodSeconds) * 1000L;
        reminderTimer.scheduleAtFixedRate(
                new ReminderTask(appointmentSupplier), periodMillis, periodMillis);

        System.out.printf("  [Reminders] Background scheduler started (every %ds, daemon thread).%n",
                periodSeconds);
    }

    /** Stops the reminder sweep and releases the timer thread. */
    public void stopReminderScheduler() {
        if (reminderTimer != null) {
            reminderTimer.cancel();
            reminderTimer = null;
            System.out.println("  [Reminders] Background scheduler stopped.");
        }
    }

    public boolean isSchedulerRunning() {
        return reminderTimer != null;
    }

    /** Prints which channels are currently subscribed. */
    public void printObservers() {
        if (observers.isEmpty()) {
            System.out.println("  No notification channels registered.");
            return;
        }
        StringBuilder sb = new StringBuilder(160);
        sb.append("  Registered channels: ");
        for (AppointmentObserver observer : observers) {
            sb.append(observer.getObserverName()).append("  ");
        }
        sb.append("\n  Events dispatched: ").append(eventsDispatched.get());
        System.out.println(sb);
    }
}
