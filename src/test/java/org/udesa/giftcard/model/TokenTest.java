package org.udesa.giftcard.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class TokenTest {

    @Test public void test01TokenEsValidoAlMomentoDeEmision() {
        Instant issued = Instant.parse("2025-01-01T12:00:00Z");
        Token tok = Token.issuedFor("alice", "t-1", issued);

        assertDoesNotThrow(() -> tok.assertValidAt(issued));
    }

    @Test public void test02TokenSigueSiendoValidoExactamenteALos5Minutos() {
        Instant issued = Instant.parse("2025-01-01T12:00:00Z");
        Token tok = Token.issuedFor("alice", "t-1", issued);

        assertDoesNotThrow(() -> tok.assertValidAt(issued.plus(Duration.ofMinutes(5))));
    }

    @Test public void test03TokenExpiraAlSuperarLos5Minutos() {
        Instant issued = Instant.parse("2025-01-01T12:00:00Z");
        Token tok = Token.issuedFor("alice", "t-1", issued);

        assertThrowsLike(() -> tok.assertValidAt(issued.plus(Duration.ofMinutes(5)).plusSeconds(1)),
                Token.Expired);
    }

    @Test public void test04ExponeUsuarioYValor() {
        Instant issued = Instant.parse("2025-01-01T12:00:00Z");
        Token tok = Token.issuedFor("bob", "tok-123", issued);

        assertEquals("bob", tok.userId());
        assertEquals("tok-123", tok.value());
    }

    // ===== helper de estilo "TusLibros" =====
    private void assertThrowsLike(Executable executable, String message) {
        assertEquals(message, assertThrows(Exception.class, executable).getMessage());
    }
}
