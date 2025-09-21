package java.org.udesa.giftcard.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Clock {
    public LocalDate today() {
        return LocalDate.now();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}