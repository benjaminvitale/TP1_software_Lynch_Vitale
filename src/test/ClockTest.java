package org.udesa.tuslibros.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestClock extends Clock {
    private LocalDateTime now;
    private LocalDate today;

    public static TestClock fixedAt(LocalDateTime now) {
        return new TestClock(now, now.toLocalDate());
    }
    public static TestClock fixedAt(LocalDate date) {
        return new TestClock(date.atStartOfDay(), date);
    }

    public TestClock(LocalDateTime now, LocalDate today) {
        this.now = now;
        this.today = today;
    }

    @Override public LocalDate today() { return today; }
    @Override public LocalDateTime now() { return now; }

    public void setNow(LocalDateTime newNow) {
        this.now = newNow;
        this.today = newNow.toLocalDate();
    }
    public void advanceMinutes(long minutes) { setNow(now.plusMinutes(minutes)); }
    public void advanceSeconds(long seconds) { setNow(now.plusSeconds(seconds)); }
    public void advanceHours(long hours) { setNow(now.plusHours(hours)); }
}
