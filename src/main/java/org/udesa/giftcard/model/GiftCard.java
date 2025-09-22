package org.udesa.giftcard.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GiftCard {
    public static final String AlreadyClaimed = "Gift card already claimed";
    public static final String NotClaimed = "Gift card not claimed";
    public static final String NotEnoughBalance = "Insufficient balance";

    private final String id;
    private String ownerUserId; // null si aún no reclamada
    private BigDecimal balance;
    private final List<Movement> movements = new ArrayList<>();

    public static GiftCard identifiedWithBalance(String id, BigDecimal initial) {
        return new GiftCard(id, normalize(initial));
    }

    private GiftCard(String id, BigDecimal initial) {
        this.id = id;
        this.balance = initial;
    }

    private static BigDecimal normalize(BigDecimal x) {
        return x.setScale(2, RoundingMode.UNNECESSARY);
    }

    public GiftCard claim(String userId) {
        if (isClaimed() && !ownerUserId.equals(userId)) throw new RuntimeException(AlreadyClaimed);
        ownerUserId = userId;
        return this;
    }

    /** Ahora recibe merchantId (String) en lugar de Merchant */
    public GiftCard charge(String merchantId, BigDecimal amount, String description, Instant when) {
        assertClaimed();
        BigDecimal norm = normalize(amount);
        if (balance.compareTo(norm) < 0) throw new RuntimeException(NotEnoughBalance);
        balance = balance.subtract(norm);
        movements.add(Movement.debit(when , merchantId, norm, description));
        return this;
    }

    private void assertClaimed() {
        if (!isClaimed()) throw new RuntimeException(NotClaimed);
    }

    public boolean isClaimed() { return ownerUserId != null; }
    public String ownerUserId() { return ownerUserId; }
    public BigDecimal balance() { return balance; }
    public List<Movement> movements() { return List.copyOf(movements); }
    public String id() { return id; }
}