package java.org.udesa.giftcard.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class GiftCardTest {

    private GiftCard newCard(String id, String initial) {
        // TODO: adaptá a tu factory/ctor real (p.ej. GiftCard.identifiedWithBalance(...))
        return new GiftCard(id, new BigDecimal(initial).setScale(2, RoundingMode.UNNECESSARY));
    }
    private Merchant newMerchant(String id) {
        // TODO: adaptá a tu Merchant real
        return new Merchant(id);
    }

    @Test
    void card_nueva_tiene_balance_inicial_y_no_reclamada() {
        GiftCard c = newCard("CARD-1", "100.00");
        assertEquals(new BigDecimal("100.00"), c.balance());
        assertFalse(c.isClaimed());
    }

    @Test
    void claim_ok_card_libre() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        assertTrue(c.isClaimed());
        assertEquals("alice", c.ownerUserId());
    }

    @Test
    void claim_falla_si_ya_reclamada_por_otro_usuario() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("bob");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> c.claim("alice"));
        // TODO: si tenés constante: assertEquals(GiftCard.AlreadyClaimed, ex.getMessage());
    }

    @Test
    void no_se_puede_cobrar_si_no_esta_reclamada() {
        GiftCard c = newCard("CARD-1", "100.00");
        Merchant m = newMerchant("M-001");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> c.charge(m, new BigDecimal("10.00"), "compra", LocalDateTime.now()));
        // TODO: si tenés constante: assertEquals(GiftCard.NotClaimed, ex.getMessage());
    }

    @Test
    void charge_descuenta_del_balance_y_agrega_movimiento() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        Merchant m = newMerchant("M-001");

        c.charge(m, new BigDecimal("30.00"), "compra", LocalDateTime.of(2025,1,1,12,0));

        assertEquals(new BigDecimal("70.00"), c.balance());
        assertEquals(1, c.movements().size());
        Movement mv = c.movements().get(0);
        assertEquals(new BigDecimal("30.00"), mv.amount());
        assertEquals("M-001", mv.merchantId());
        assertEquals("compra", mv.description());
    }

    @Test
    void rechaza_cargo_por_saldo_insuficiente() {
        GiftCard c = newCard("CARD-2", "20.00");
        c.claim("alice");
        Merchant m = newMerchant("M-001");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> c.charge(m, new BigDecimal("25.00"), "compra", LocalDateTime.now()));
        // TODO: si tenés constante: assertEquals(GiftCard.NotEnoughBalance, ex.getMessage());
    }

    @Test
    void movimientos_quedan_en_orden_cronologico_de_carga() {
        GiftCard c = newCard("CARD-1", "100.00");
        c.claim("alice");
        Merchant m = newMerchant("M-001");

        c.charge(m, new BigDecimal("10.00"), "m1", LocalDateTime.of(2025,1,1,10,0));
        c.charge(m, new BigDecimal("5.00"), "m2", LocalDateTime.of(2025,1,1,11,0));

        assertEquals("m1", c.movements().get(0).description());
        assertEquals("m2", c.movements().get(1).description());
    }
}
