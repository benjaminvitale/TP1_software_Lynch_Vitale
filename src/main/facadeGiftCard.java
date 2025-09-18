package org.udesa.tuslibros.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class GiftCardsSystemFacade {
    // ===== Mensajes de error (mismo estilo que TusLibros) =====
    public static String invalidUserAndOrPasswordErrorDescription = "Invalid user and/or password";
    public static String invalidTokenErrorDescription = "Invalid token";
    public static String tokenHasExpiredErrorDescription = "Token has expired";
    public static String invalidCardIdErrorDescription = "Invalid gift card id";
    public static String cardAlreadyClaimedErrorDescription = "Gift card already claimed";
    public static String cardNotClaimedByUserErrorDescription = "Gift card not claimed by user";
    public static String invalidMerchantErrorDescription = "Invalid merchant";
    public static String notEnoughBalanceErrorDescription = "Insufficient balance";

    // ===== Config =====
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    // ===== Estado (precargado) =====
    private final Map<String, String> validUsers;               // username -> password
    private final Map<String, GiftCard> giftCards;              // cardId -> GiftCard
    private final Map<String, Merchant> merchantsById;          // merchantId -> Merchant
    private final Map<String, String> merchantApiKeys;          // merchantId -> apiKey (o public key id)
    private final Map<String, Token> activeTokensByValue = new HashMap<>(); // tokenValue -> Token

    private final Clock clock;

    public GiftCardsSystemFacade(Map<String, String> validUsers,
                                 Map<String, GiftCard> giftCards,
                                 Map<String, Merchant> merchantsById,
                                 Map<String, String> merchantApiKeys,
                                 Clock clock) {
        this.validUsers = validUsers;
        this.giftCards = giftCards;
        this.merchantsById = merchantsById;
        this.merchantApiKeys = merchantApiKeys;
        this.clock = clock;
    }

    // ==================== AUTH / TOKEN ====================

    /**
     * Login simple que emite un token que vence a los 5'. Si luego querés el handshake asimétrico,
     * reemplazá la generación del token por tu clave simétrica derivada (base64) y dejá igual el resto.
     */
    public String loginFor(String user, String pass /*, PublicKey clientPub, ... */) {
        checkValidUser(user, pass);

        // Token simple por ahora (UUID base64). Si hacés ECDH/HKDF, meté la K acá.
        String tokenValue = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
        Token token = new Token(tokenValue, user, clock.now()); // adaptar si tu Token tiene otra firma
        activeTokensByValue.put(tokenValue, token);
        return tokenValue;
    }

    private void checkValidUser(String user, String pass) {
        if (!Objects.equals(pass, validUsers.get(user))) {
            throw new RuntimeException(invalidUserAndOrPasswordErrorDescription);
        }
    }

    private String userIdFromValidToken(String tokenValue) {
        Token token = activeTokensByValue.get(tokenValue);
        if (token == null) throw new RuntimeException(invalidTokenErrorDescription);

        LocalDateTime issuedAt = token.issuedAt(); // adaptar si tu Token expone distinto
        if (issuedAt.plus(TOKEN_TTL).isBefore(clock.now())) {
            activeTokensByValue.remove(tokenValue); // opcional: limpiar
            throw new RuntimeException(tokenHasExpiredErrorDescription);
        }
        return token.userId(); // adaptar si tu Token expone distinto
    }

    // ==================== CLAIM / CONSULTAS ====================

    public void claimCardIdentifiedAs(String token, String cardId) {
        String userId = userIdFromValidToken(token);
        GiftCard card = cardIdentifiedAs(cardId);

        if (card.isClaimed() && !userId.equals(card.ownerUserId())) { // adaptar getters si difieren
            throw new RuntimeException(cardAlreadyClaimedErrorDescription);
        }

        card.claim(userId);  // adaptar si tu GiftCard usa otra firma
        giftCards.put(cardId, card);
    }

    public BigDecimal balanceOfCardIdentifiedAs(String token, String cardId) {
        String userId = userIdFromValidToken(token);
        GiftCard card = mustBeMine(cardId, userId);
        return card.balance(); // BigDecimal con scale=2 recomendado
    }

    public List<Movement> movementsOfCardIdentifiedAs(String token, String cardId) {
        String userId = userIdFromValidToken(token);
        GiftCard card = mustBeMine(cardId, userId);
        return card.movements(); // List<Movement> inmutable o copia
    }

    // ==================== CARGO DEL MERCHANT (entrante) ====================

    /**
     * El merchant notifica: id_merchant, id_usuario, giftcard_id, monto.
     * Además manda su credencial (apiKey) para autenticar la llamada.
     */
    public void chargeNotifiedByMerchant(String idMerchant,
                                         String merchantApiKey,
                                         String idUsuario,
                                         String giftcardId,
                                         BigDecimal monto) {
        // 1) Autenticación del merchant
        Merchant merchant = merchantsById.get(idMerchant);
        if (merchant == null) throw new RuntimeException(invalidMerchantErrorDescription);

        String expectedKey = merchantApiKeys.get(idMerchant);
        if (!Objects.equals(expectedKey, merchantApiKey)) throw new RuntimeException(invalidMerchantErrorDescription);

        // 2) Validaciones de la card y el dueño
        GiftCard card = cardIdentifiedAs(giftcardId);
        if (!card.isClaimed() || !idUsuario.equals(card.ownerUserId())) {
            throw new RuntimeException(cardNotClaimedByUserErrorDescription);
        }

        // 3) Normalizar monto y validar saldo (si no se permite negativo)
        BigDecimal norm = monto.setScale(2, RoundingMode.UNNECESSARY);
        if (card.balance().compareTo(norm) < 0) throw new RuntimeException(notEnoughBalanceErrorDescription);

        // 4) Aplicar cargo
        // Si tu GiftCard.charge requiere descripción, mandá algo fijo o parámetro
        card.charge(merchant, norm, "merchant charge", clock.now()); // adaptar firma si es distinta
        giftCards.put(giftcardId, card);
    }

    // ==================== Helpers ====================

    private GiftCard cardIdentifiedAs(String cardId) {
        GiftCard card = giftCards.get(cardId);
        if (card == null) throw new RuntimeException(invalidCardIdErrorDescription);
        return card;
    }

    private GiftCard mustBeMine(String cardId, String userId) {
        GiftCard card = cardIdentifiedAs(cardId);
        if (!card.isClaimed() || !userId.equals(card.ownerUserId())) {
            throw new RuntimeException(cardNotClaimedByUserErrorDescription);
        }
        return card;
    }

    // ==================== Getters (si necesitás en tests) ====================

    public Clock clock() { return clock; }
    public Map<String, GiftCard> giftCards() { return giftCards; }
    public Map<String, Token> activeTokens() { return activeTokensByValue; }
}
