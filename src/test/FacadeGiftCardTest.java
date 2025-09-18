package org.udesa.tuslibros.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GiftCardsSystemFacadeTest {

    private Map<String, String> validUsers;
    private Map<String, GiftCard> giftCards;
    private Map<String, Merchant> merchantsById;
    private Map<String, String> merchantApiKeys;
    private TestClock clock;
    private GiftCardsSystemFacade facade;

    // Helpers de dominio (adaptá a tus constructores)
    private static GiftCard newCard(String id, String initial) {
        // TODO: reemplazá por tu forma real de crear GiftCard (ej: GiftCard.identifiedWithBalance(id, new BigDecimal(initial)))
        return new GiftCard(id, new BigDecimal(initial)); // si tu clase difiere, cambialo
    }
    private static Merchant newMerchant(String id) {
        // TODO: reemplazá por tu Merchant real (si necesitás más campos)
        return new Merchant(id);
    }

    @BeforeEach
    void setUp() {
        validUsers = new HashMap<>();
        validUsers.put("alice", "pwd");
        validUsers.put("bob", "secret");

        giftCards = new HashMap<>();
        giftCards.put("CARD-1", newCard("CARD-1", "100.00"));
        giftCards.put("CARD-2", newCard("CARD-2", "50.00"));

        merchantsById = new HashMap<>();
        merchantsById.put("M-001", newMerchant("M-001"));

        merchantApiKeys = new HashMap<>();
        merchantApiKeys.put("M-001", "KEY-001");

        clock = TestClock.fixedAt(LocalDateTime.of(2025, 1, 1, 12, 0, 0));

        facade = new GiftCardsSystemFacade(
                validUsers, giftCards, merchantsById, merchantApiKeys, clock
        );
    }

    @Test
    void login_ok_emite_token() {
        String token = facade.loginFor("alice", "pwd");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void login_falla_credenciales_invalidas() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.loginFor("alice", "wrong"));
        assertEquals(GiftCardsSystemFacade.invalidUserAndOrPasswordErrorDescription, ex.getMessage());
    }

    @Test
    void token_expira_a_los_5_minutos() {
        String token = facade.loginFor("alice", "pwd");
        // Reclamo ok dentro de ventana
        facade.claimCardIdentifiedAs(token, "CARD-1");

        // Adelanto 6 minutos ⇒ token vencido
        clock.advanceMinutes(6);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.balanceOfCardIdentifiedAs(token, "CARD-1"));
        assertEquals(GiftCardsSystemFacade.tokenHasExpiredErrorDescription, ex.getMessage());
    }

    @Test
    void claim_ok_card_libre() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-2");

        // Verifica dueño
        GiftCard card = giftCards.get("CARD-2");
        assertTrue(card.isClaimed());
        assertEquals("alice", card.ownerUserId()); // adaptá si tu getter difiere
    }

    @Test
    void claim_falla_si_card_ya_es_de_otro_usuario() {
        // Bob reclama primero
        String bobToken = facade.loginFor("bob", "secret");
        facade.claimCardIdentifiedAs(bobToken, "CARD-1");

        // Alice intenta reclamar la misma
        String aliceToken = facade.loginFor("alice", "pwd");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.claimCardIdentifiedAs(aliceToken, "CARD-1"));
        assertEquals(GiftCardsSystemFacade.cardAlreadyClaimedErrorDescription, ex.getMessage());
    }

    @Test
    void merchant_valido_cobra_card_del_usuario() {
        // Claim por Alice
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        BigDecimal balanceInicial = giftCards.get("CARD-1").balance();
        BigDecimal monto = new BigDecimal("30.00").setScale(2, RoundingMode.UNNECESSARY);

        // Merchant carga
        facade.chargeNotifiedByMerchant("M-001", "KEY-001", "alice", "CARD-1", monto);

        BigDecimal balanceFinal = giftCards.get("CARD-1").balance();
        assertEquals(balanceInicial.subtract(monto), balanceFinal);
        assertFalse(giftCards.get("CARD-1").movements().isEmpty());
    }

    @Test
    void rechaza_cargo_por_saldo_insuficiente() {
        // Alice reclama CARD-2 (tiene 50.00)
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-2");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.chargeNotifiedByMerchant("M-001", "KEY-001", "alice", "CARD-2", new BigDecimal("60.00")));
        assertEquals(GiftCardsSystemFacade.notEnoughBalanceErrorDescription, ex.getMessage());
    }

    @Test
    void rechaza_cargo_si_card_no_es_del_usuario() {
        // Bob reclama CARD-1
        String bobToken = facade.loginFor("bob", "secret");
        facade.claimCardIdentifiedAs(bobToken, "CARD-1");

        // Merchant intenta cobrar a nombre de Alice (mismatch)
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.chargeNotifiedByMerchant("M-001", "KEY-001", "alice", "CARD-1", new BigDecimal("10.00")));
        assertEquals(GiftCardsSystemFacade.cardNotClaimedByUserErrorDescription, ex.getMessage());
    }

    @Test
    void rechaza_cargo_si_merchant_invalido() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.chargeNotifiedByMerchant("M-XXX", "KEY-001", "alice", "CARD-1", new BigDecimal("10.00")));
        assertEquals(GiftCardsSystemFacade.invalidMerchantErrorDescription, ex.getMessage());
    }

    @Test
    void rechaza_cargo_si_apiKey_invalida() {
        String token = facade.loginFor("alice", "pwd");
        facade.claimCardIdentifiedAs(token, "CARD-1");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> facade.chargeNotifiedByMerchant("M-001", "BAD-KEY", "alice", "CARD-1", new BigDecimal("10.00")));
        assertEquals(GiftCardsSystemFacade.invalidMerchantErrorDescription, ex.getMessage());
    }
}
