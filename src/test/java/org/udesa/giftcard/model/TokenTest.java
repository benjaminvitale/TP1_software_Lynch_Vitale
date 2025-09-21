package java.org.udesa.giftcard.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TokenTest {

    @Test
    void token_no_expira_antes_de_5_minutos() {
        LocalDateTime t0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
        // TODO: adaptá al constructor/factory real de tu Token:
        Token tok = new Token("tok", "alice", t0);

        // Suponemos regla: expira ESTRICTAMENTE después de 5'
        assertFalse(tok.expiredOn(t0.plusMinutes(5)));             // límite incluido (aún válido)
        assertTrue(tok.expiredOn(t0.plusMinutes(5).plusSeconds(1))); // ya expiró
    }

    @Test
    void token_expira_exactamente_al_superar_ttl() {
        LocalDateTime issued = LocalDateTime.of(2025, 2, 1, 10, 0, 0);
        Token tok = new Token("tok", "alice", issued);

        assertFalse(tok.expiredOn(issued.plus(Duration.ofMinutes(4)).plusSeconds(59)));
        assertTrue(tok.expiredOn(issued.plus(Duration.ofMinutes(5)).plusSeconds(1)));
    }

    @Test
    void token_exponde_usuario_y_fecha_de_emision() {
        LocalDateTime issued = LocalDateTime.of(2025, 3, 1, 9, 30, 0);
        Token tok = new Token("tok", "alice", issued);

        assertEquals("alice", tok.userId());
        assertEquals(issued, tok.issuedAt());
    }
}
