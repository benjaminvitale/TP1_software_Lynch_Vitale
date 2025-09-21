package java.org.udesa.giftcard.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Session {
    public static String invalidUserAndOrPasswordErrorDescription = "Invalid user and/or password";
    public static String invalidTokenErrorDescription = "Invalid token";
    public static String tokenHasExpiredErrorDescription = "Token has expired";

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
        String tokenValue = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
        Token token = new Token(tokenValue, user, clock.now()); // adapta si tu Token tiene otra firma
        activeTokensByValue.put(tokenValue, token);
        return tokenValue;
    }

    public String userIdFromValidToken(String tokenValue) {
        Token token = activeTokensByValue.get(tokenValue);
        if (token == null) throw new RuntimeException(invalidTokenErrorDescription);

        LocalDateTime issuedAt = token.issuedAt();
        if (issuedAt.plus(TOKEN_TTL).isBefore(clock.now())) {
            activeTokensByValue.remove(tokenValue); // opcional: limpieza
            throw new RuntimeException(tokenHasExpiredErrorDescription);
        }
        return token.userId();
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
