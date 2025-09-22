package org.udesa.giftcard.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class GiftCardTest {

    // ===== helpers =====
    private static GiftCard newCard(String id, String amount) {
        return GiftCard.identifiedWithBalance(id, new BigDecimal(amount));
    }
    private void assertThrowsLike(Executable ex, String message) {
        assertEquals(message, assertThrows(Exception.class, ex).getMessage());
    }
    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    // ===== tests =====

    @Test public void test01CardNuevaTieneBalanceInicialYNoReclamada() {
        GiftCard c = newCard("CARD-1", "100.00");
        assertEquals(new BigDecimal("100.00"), c.balance());
        assertFalse(c.isClaimed());
        assertNull(c.ownerUserId());
    }

    @Test public void test02ClaimOkCardLibre() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        assertTrue(c.isClaimed());
        assertEquals("alice", c.ownerUserId());
    }

    @Test public void test03ClaimMismoUsuarioEsIdempotente() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        // no debería fallar si reclama de nuevo el mismo usuario
        c.claim("alice");
        assertEquals("alice", c.ownerUserId());
    }

    @Test public void test04ClaimFallaSiCardYaEsDeOtroUsuario() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("bob");
        assertThrowsLike(() -> c.claim("alice"), GiftCard.AlreadyClaimed);
    }

    @Test public void test05NoSePuedeCobrarSiNoEstaReclamada() {
        GiftCard c = newCard("CARD-1", "100.00");
        assertThrowsLike(
                () -> c.charge("M-001", new BigDecimal("10.00"), "compra", Instant.parse("2025-01-01T12:00:00Z")),
                GiftCard.NotClaimed
        );
    }

    @Test public void test06ChargeDescuentaBalanceYAgregaMovimiento() throws Exception {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");

        Instant when = Instant.parse("2025-01-01T12:00:00Z");
        c.charge("M-001", new BigDecimal("30.00"), "almuerzo", when);

        assertEquals(new BigDecimal("70.00"), c.balance());
        assertEquals(1, c.movements().size());

        // inspecciono el Movement por reflexión (no hay getters)
        Movement mv = c.movements().get(0);
        assertEquals(when,       readField(mv, "when"));
        assertEquals("M-001",    readField(mv, "merchantId"));
        assertEquals(new BigDecimal("30.00"), readField(mv, "amount"));
        assertEquals("almuerzo", readField(mv, "description"));
    }

    @Test public void test07RechazaCargoPorSaldoInsuficiente() {
        GiftCard c = newCard("CARD-2", "20.00");
        c.claim("alice");
        assertThrowsLike(
                () -> c.charge("M-001", new BigDecimal("25.00"), "compra", Instant.now()),
                GiftCard.NotEnoughBalance
        );
    }

    @Test public void test08MontoConMasDe2DecimalesLanzaArithmeticException() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        assertThrows(ArithmeticException.class,
                () -> c.charge("M-001", new BigDecimal("10.001"), "compra", Instant.now()));
    }

    @Test public void test09IdSeExponeCorrectamente() {
        GiftCard c = newCard("CARD-XYZ", "5");
        assertEquals("CARD-XYZ", c.id());
    }
}
