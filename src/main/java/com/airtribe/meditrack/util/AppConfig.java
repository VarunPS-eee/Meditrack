package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public final class AppConfig {

    /** Eagerly created — the JVM's class-init lock does the synchronisation for us. */
    private static final AppConfig INSTANCE = new AppConfig();

    private final Map<String, String> settings;
    private final LocalDateTime startedAt;
    private boolean persistenceEnabled;
    private boolean remindersEnabled;
    private boolean verboseMode;

    /** Private — no caller can construct a second instance. */
    private AppConfig() {
        this.startedAt = LocalDateTime.now();
        this.settings = new LinkedHashMap<>();
        this.persistenceEnabled = true;
        this.remindersEnabled = true;
        this.verboseMode = false;

        settings.put("app.name", Constants.APP_NAME);
        settings.put("app.version", Constants.APP_VERSION);
        settings.put("app.org", Constants.ORG_NAME);
        settings.put("data.dir", Constants.DATA_DIR);
        settings.put("tax.rate", String.valueOf(Constants.TAX_RATE));
        settings.put("clinic.hours", Constants.CLINIC_OPEN_HOUR + ":00-" + Constants.CLINIC_CLOSE_HOUR + ":00");
        settings.put("slot.minutes", String.valueOf(Constants.SLOT_DURATION_MINUTES));
        settings.put("java.version", System.getProperty("java.version"));
        settings.put("os.name", System.getProperty("os.name"));

        System.out.println("[AppConfig] Eager singleton constructed at " + DateUtil.format(startedAt));
    }

    /**
     * @return the one and only configuration instance
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String get(String key) {
        return settings.get(key);
    }

    public String get(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        if (key != null && !key.isBlank()) {
            settings.put(key, value);
        }
    }

    /** @return an unmodifiable view of all settings */
    public Map<String, String> getAllSettings() {
        return Collections.unmodifiableMap(settings);
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public boolean isVerboseMode() {
        return verboseMode;
    }

    public void setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** @return how long this JVM has been running, in seconds */
    public long getUptimeSeconds() {
        return java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
    }

    /**
     * Prints the active configuration — used by the {@code Settings} menu option.
     */
    public void printConfiguration() {
        StringBuilder sb = new StringBuilder(400);
        sb.append("\n  Active configuration\n")
                .append("  ").append("-".repeat(46)).append('\n');
        settings.forEach((k, v) -> sb.append(String.format("    %-16s : %s%n", k, v)));
        sb.append(String.format("    %-16s : %s%n", "persistence", persistenceEnabled ? "ON" : "OFF"))
                .append(String.format("    %-16s : %s%n", "reminders", remindersEnabled ? "ON" : "OFF"))
                .append(String.format("    %-16s : %s%n", "verbose", verboseMode ? "ON" : "OFF"))
                .append(String.format("    %-16s : %ds%n", "uptime", getUptimeSeconds()));
        System.out.println(sb);
    }
}
