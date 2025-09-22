package org.udesa.giftcard.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

public class Clock {

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public Instant nowInstant()     { return Instant.now(); }
}