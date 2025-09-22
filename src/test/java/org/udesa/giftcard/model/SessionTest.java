package org.udesa.giftcard.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    private Map<String, String> users;
    private TestClock clock;
    private Session session;

    @BeforeEach
    void setUp() {
        users = new HashMap<>();
        users.put("alice", "pwd");
        users.put("bob", "secret");

        clock = TestClock.fixedAt(Instant.parse("2025-01-01T12:00:00Z"));
        session = new Session(users, clock);
    }

    @Test public void test01LoginOkEmiteTokenNoVacio() {
        String token = session.loginFor("alice", "pwd");
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(session.isValid(token));
    }

    @Test public void test02LoginFallaConCredencialesInvalidas() {
        assertThrowsLike(
                () -> session.loginFor("alice", "WRONG"),
                Session.invalidUserAndOrPasswordErrorDescription
        );
    }

    @Test public void test03TokenValidoHasta5MinutosYLuegoExpira() {
        String token = session.loginFor("alice", "pwd");

        // A los 5' exactos sigue siendo válido
        clock.advanceMinutes(5);
        assertTrue(session.isValid(token));

        // 5' + 1s => expira
        clock.advanceSeconds(1);
        assertThrowsLike(
                () -> session.userIdFromValidToken(token),
                Session.tokenHasExpiredErrorDescription
        );
        assertFalse(session.isValid(token));
    }

    @Test public void test04UserIdFromValidTokenDevuelveUsuario() {
        String token = session.loginFor("bob", "secret");
        assertEquals("bob", session.userIdFromValidToken(token));
    }

    @Test public void test05TokenInexistenteLanzaInvalidToken() {
        assertThrowsLike(
                () -> session.userIdFromValidToken("no-such-token"),
                Session.invalidTokenErrorDescription
        );
    }


    private void assertThrowsLike(Executable executable, String message) {
        assertEquals(message, assertThrows(Exception.class, executable).getMessage());
    }

    // ===== Test clock seteable =====
    static class TestClock extends Clock {
        private Instant now;

        static TestClock fixedAt(Instant instant) { return new TestClock(instant); }
        TestClock(Instant instant) { this.now = instant; }

        @Override public Instant nowInstant() { return now; }
        void advanceMinutes(long minutes) { now = now.plus(Duration.ofMinutes(minutes)); }
        void advanceSeconds(long seconds) { now = now.plusSeconds(seconds); }
    }
}
