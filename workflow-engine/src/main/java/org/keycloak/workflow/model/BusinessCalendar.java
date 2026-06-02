package org.keycloak.workflow.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Configurable business-day calendar used to compute SLA deadlines (FR-2.2).
 * Per-realm; if the workflow has no calendar bound, deadlines are 24/7.
 */
public class BusinessCalendar {

    private String id;
    private String realmId;
    private String name;
    private ZoneId zoneId = ZoneId.systemDefault();
    private Set<DayOfWeek> workingDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    private Set<LocalDate> holidays = new HashSet<>();

    /** Adds {@code minutes} business-minutes to {@code from}, skipping non-working days. */
    public ZonedDateTime addBusinessMinutes(ZonedDateTime from, long minutes) {
        ZonedDateTime cursor = from;
        long remaining = minutes;
        while (remaining > 0) {
            if (isWorkingDay(cursor.toLocalDate())) {
                long minutesInDay = Duration.between(cursor, cursor.toLocalDate().plusDays(1).atStartOfDay(zoneId)).toMinutes();
                if (remaining <= minutesInDay) {
                    return cursor.plusMinutes(remaining);
                }
                remaining -= minutesInDay;
            }
            cursor = cursor.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        }
        return cursor;
    }

    public boolean isWorkingDay(LocalDate d) {
        return workingDays.contains(d.getDayOfWeek()) && !holidays.contains(d);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public ZoneId getZoneId() { return zoneId; }
    public void setZoneId(ZoneId z) { this.zoneId = z; }
    public Set<DayOfWeek> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<DayOfWeek> w) { this.workingDays = w; }
    public Set<LocalDate> getHolidays() { return holidays; }
    public void setHolidays(Set<LocalDate> h) { this.holidays = h; }
}
