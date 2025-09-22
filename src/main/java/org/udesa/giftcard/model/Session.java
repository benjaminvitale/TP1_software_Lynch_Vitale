package org.udesa.giftcard.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Session {
    public static String invalidUserAndOrPasswordErrorDescription = "Invalid user and/or password";
    public static String invalidTokenErrorDescription = "Invalid token";
    public static String tokenHasExpiredErrorDescription = "Token expired";

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final Map<String, String> validUsers;          // username -> password
    private final Map<String, Token> activeTokensByValue = new HashMap<>();
    private final Clock clock;

    public Session(Map<String, String> validUsers, Clock clock) {
        this.validUsers = validUsers;
        this.clock = clock;
    }

    public String loginFor(String user, String pass) {
        checkValidUser(user, pass);
        String tokenValue = java.util.UUID.randomUUID().toString();
        Token t = Token.issuedFor(user, tokenValue, clock.nowInstant());
        activeTokensByValue.put(t.value(), t);
        return t.value();
    }

    public String userIdFromValidToken(String tokenValue) {
        Token t = activeTokensByValue.get(tokenValue);
        if (t == null) throw new RuntimeException(invalidTokenErrorDescription);
        t.assertValidAt(clock.nowInstant());
        return t.userId();
    }

    public boolean isValid(String tokenValue) {
        try { userIdFromValidToken(tokenValue); return true; }
        catch (RuntimeException ex) { return false; }
    }

    private void checkValidUser(String user, String pass) {
        if (!Objects.equals(pass, validUsers.get(user))) {
            throw new RuntimeException(invalidUserAndOrPasswordErrorDescription);
        }
    }
}
