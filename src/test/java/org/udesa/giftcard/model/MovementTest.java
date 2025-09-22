package org.udesa.giftcard.model;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class MovementTest {

    @Test
    public void test01DebitCreaUnMovimientoConLosValoresEsperados() throws Exception {
        Instant when = Instant.parse("2025-01-01T12:00:00Z");
        String merchantId = "M-001";
        BigDecimal amount = new BigDecimal("10.00");
        String description = "almuerzo";

        Movement mv = Movement.debit(when, merchantId, amount, description);

        assertEquals(when,      readField(mv, "when"));
        assertEquals(merchantId,readField(mv, "merchantId"));
        assertEquals(amount,    readField(mv, "amount"));
        assertEquals(description,readField(mv, "description"));
    }

    @Test
    public void test02ClaseEsFinalYCamposSonPrivateFinalYConstructorEsPrivate() throws Exception {
        // clase final
        assertTrue(Modifier.isFinal(Movement.class.getModifiers()));

        // único constructor, y private
        var ctors = Movement.class.getDeclaredConstructors();
        assertEquals(1, ctors.length);
        assertTrue(Modifier.isPrivate(ctors[0].getModifiers()));

        // campos private final
        assertPrivateFinal("when");
        assertPrivateFinal("merchantId");
        assertPrivateFinal("amount");
        assertPrivateFinal("description");
    }

    @Test
    public void test03DosDebitDevuelvenInstanciasDistintas() {
        Movement a = Movement.debit(Instant.EPOCH, "M-001", new BigDecimal("1.00"), "a");
        Movement b = Movement.debit(Instant.EPOCH, "M-001", new BigDecimal("1.00"), "a");
        assertNotSame(a, b);
    }

    // ===== helpers =====
    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    private static void assertPrivateFinal(String fieldName) throws Exception {
        Field f = Movement.class.getDeclaredField(fieldName);
        int m = f.getModifiers();
        assertTrue(Modifier.isPrivate(m), fieldName + " debería ser private");
        assertTrue(Modifier.isFinal(m),   fieldName + " debería ser final");
    }
}
