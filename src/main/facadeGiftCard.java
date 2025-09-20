package org.udesa.tuslibros.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class GiftCardsSystemFacade {
    public static String invalidUserAndOrPasswordErrorDescription = "Invalid user and/or password";
    public static String invalidCardIdErrorDescription = "Invalid gift card id";
    public static String cardAlreadyClaimedErrorDescription = "Gift card already claimed";
    public static String cardNotClaimedByUserErrorDescription = "Gift card not claimed by user";
    public static String invalidMerchantErrorDescription = "Invalid merchant";
    public static String notEnoughBalanceErrorDescription = "Insufficient balance";

    private final Map<String, GiftCard> giftCards;   // cardId -> GiftCard
    private final Set<String> validMerchantIds;      // << ahora solo IDs válidos
    private final Session session;                   // login + validación de token

    public GiftCardsSystemFacade(Map<String, String> validUsers,
                                 Map<String, GiftCard> giftCards,
                                 Set<String> validMerchantIds,
                                 Clock clock) {
        this.giftCards = giftCards;
        this.validMerchantIds = validMerchantIds;
        this.session = new Session(validUsers, clock);
    }

    // ===== AUTH =====
    public String loginFor(String user, String pass) {
        return session.loginFor(user, pass);
    }

    // ===== CLAIM / CONSULTAS =====
    public void claimCardIdentifiedAs(String token, String cardId) {
        String userId = session.userIdFromValidToken(token);
        GiftCard card = cardIdentifiedAs(cardId);

        if (card.isClaimed() && !userId.equals(card.ownerUserId()))
            throw new RuntimeException(cardAlreadyClaimedErrorDescription);

        card.claim(userId);
        giftCards.put(cardId, card);
    }

    public BigDecimal balanceOfCardIdentifiedAs(String token, String cardId) {
        String userId = session.userIdFromValidToken(token);
        GiftCard card = mustBeMine(cardId, userId);
        return card.balance();
    }

    public List<Movement> movementsOfCardIdentifiedAs(String token, String cardId) {
        String userId = session.userIdFromValidToken(token);
        GiftCard card = mustBeMine(cardId, userId);
        return card.movements();
    }

    // ===== CARGO DEL MERCHANT (entrante) =====
    public void chargeNotifiedByMerchant(String idMerchant,
                                         String idUsuario,
                                         String giftcardId,
                                         BigDecimal monto) {
        if (!validMerchantIds.contains(idMerchant))
            throw new RuntimeException(invalidMerchantErrorDescription);

        GiftCard card = cardIdentifiedAs(giftcardId);
        if (!card.isClaimed() || !idUsuario.equals(card.ownerUserId()))
            throw new RuntimeException(cardNotClaimedByUserErrorDescription);

        BigDecimal norm = monto.setScale(2, RoundingMode.UNNECESSARY);
        if (card.balance().compareTo(norm) < 0)
            throw new RuntimeException(notEnoughBalanceErrorDescription);

        // timestamp real del sistema
        card.charge(idMerchant, norm, "merchant charge", LocalDateTime.now());
        giftCards.put(giftcardId, card);
    }

    // ===== helpers =====
    private GiftCard cardIdentifiedAs(String cardId) {
        GiftCard card = giftCards.get(cardId);
        if (card == null) throw new RuntimeException(invalidCardIdErrorDescription);
        return card;
    }

    private GiftCard mustBeMine(String cardId, String userId) {
        GiftCard card = cardIdentifiedAs(cardId);
        if (!card.isClaimed() || !userId.equals(card.ownerUserId()))
            throw new RuntimeException(cardNotClaimedByUserErrorDescription);
        return card;
    }
}
