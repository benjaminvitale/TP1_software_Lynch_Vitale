package org.udesa.tuslibros.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MovementTest {

    @Test
    void movement_debit_registra_campos_basicos() {
        LocalDateTime when = LocalDateTime.of(2025, 1, 1, 12, 0);
        Movement mv = Movement.debit(when, "M-001", new BigDecimal("15.25"), "almuerzo");
        assertEquals(when, mv.when());
        assertEquals("M-001", mv.merchantId());
        assertEquals(new BigDecimal("15.25"), mv.amount());
        assertEquals("almuerzo", mv.description());
    }

    @Test
    void movement_debe_ser_inmutable() {
        LocalDateTime when = LocalDateTime.of(2025, 1, 1, 12, 0);
        Movement mv = Movement.debit(when, "M-001", new BigDecimal("10.00"), "compra");
        // No hay setters; verificamos que getters devuelven siempre lo mismo
        assertEquals("compra", mv.description());
        assertEquals("M-001", mv.merchantId());
        assertEquals(new BigDecimal("10.00"), mv.amount());
        assertEquals(when, mv.when());
    }
}
