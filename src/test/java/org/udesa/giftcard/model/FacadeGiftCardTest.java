package org.udesa.giftcard.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FacadeGiftCardTest {

    private Map<String, String> validUsers;
    private Map<String, GiftCard> giftCards;
    private Set<String> validMerchantIds;
    private FacadeGiftCard facade;

    @BeforeEach
    public void setUp() {
        validUsers = new HashMap<>();
        validUsers.put("alice", "pwd");
        validUsers.put("bob", "secret");

        giftCards = new HashMap<>();
        giftCards.put("CARD-1", newCard("CARD-1", "100.00"));
        giftCards.put("CARD-2", newCard("CARD-2", "50.00"));

        validMerchantIds = new HashSet<>(List.of("M-001")); // merchant válido

        facade = new FacadeGiftCard(validUsers, giftCards, validMerchantIds, new Clock());
    }

    // ===== helpers de dominio =====
    private static GiftCard newCard(String id, String initial) {
        return GiftCard.identifiedWithBalance(
                id, new BigDecimal(initial).setScale(2, RoundingMode.UNNECESSARY)
        );
    }

    private void assertThrowsLike(Executable executable, String message) {
        assertEquals(message, assertThrows(Exception.class, executable).getMessage());
    }

    // ================= TESTS =================

    @Test public void test01LoginOkEmiteToken() {
        String token = facade.loginFor("alice", "pwd");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test public void test02LoginFallaConCredencialesInvalidas() {
        assertThrowsLike(
                () -> facade.loginFor("alice", "WRONG"),
                FacadeGiftCard.invalidUserAndOrPasswordErrorDescription
        );
    }

    @Test public void test03ClaimOkCardLibre() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-2");

        GiftCard c = giftCards.get("CARD-2");
        assertTrue(c.isClaimed());
        assertEquals("alice", c.ownerUserId());
    }

    @Test public void test04ClaimFallaSiCardEsDeOtroUsuario() {
        String bob = facade.loginFor("bob", "secret");
        facade.claimCardIdentifiedAs(bob, "CARD-1");

        String alice = facade.loginFor("alice", "pwd");
        assertThrowsLike(
                () -> facade.claimCardIdentifiedAs(alice, "CARD-1"),
                FacadeGiftCard.cardAlreadyClaimedErrorDescription
        );
    }

    @Test public void test05BalanceDevuelveElMontoActual() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        BigDecimal balance = facade.balanceOfCardIdentifiedAs(token, "CARD-1");
        assertEquals(new BigDecimal("100.00").setScale(2), balance);
    }

    @Test public void test06MerchantValidoPuedeCobrarCardDelUsuario() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        facade.chargeNotifiedByMerchant("M-001", "alice", "CARD-1",
                new BigDecimal("30.00"));

        assertEquals(new BigDecimal("70.00").setScale(2),
                giftCards.get("CARD-1").balance());
        assertEquals(1, giftCards.get("CARD-1").movements().size());
    }

    @Test public void test07RechazaCargoPorSaldoInsuficiente() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-2"); // 50.00

        assertThrowsLike(
                () -> facade.chargeNotifiedByMerchant("M-001", "alice", "CARD-2",
                        new BigDecimal("60.00")),
                FacadeGiftCard.notEnoughBalanceErrorDescription
        );
    }

    @Test public void test08RechazaCargoSiMerchantEsInvalido() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        assertThrowsLike(
                () -> facade.chargeNotifiedByMerchant("M-XXX", "alice", "CARD-1",
                        new BigDecimal("10.00")),
                FacadeGiftCard.invalidMerchantErrorDescription
        );
    }

    @Test public void test09RechazaCargoSiLaCardNoPerteneceAlUsuario() {
        String bob = facade.loginFor("bob", "secret");
        facade.claimCardIdentifiedAs(bob, "CARD-1"); // ahora es de Bob

        assertThrowsLike(
                () -> facade.chargeNotifiedByMerchant("M-001", "alice", "CARD-1",
                        new BigDecimal("10.00")),
                FacadeGiftCard.cardNotClaimedByUserErrorDescription
        );
    }

    @Test public void test10MovementsDevuelveLosCargosRealizados() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        facade.chargeNotifiedByMerchant("M-001", "alice", "CARD-1",
                new BigDecimal("5.00"));
        var movements = facade.movementsOfCardIdentifiedAs(token, "CARD-1");

        assertEquals(1, movements.size());
    }
}
