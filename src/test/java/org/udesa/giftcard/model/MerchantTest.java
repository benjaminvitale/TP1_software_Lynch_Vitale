package java.org.udesa.giftcard.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MerchantTest {

    @Test
    void merchant_tiene_identidad_y_opcionalmente_apiKey() {
        // TODO: adaptá a tu implementación real
        Merchant m = new Merchant("M-001"); // o new Merchant("M-001", "KEY-001")
        assertEquals("M-001", m.id());
        // si usás apiKey:
        // assertEquals("KEY-001", m.apiKey());
    }

    @Test
    void merchants_distintos_no_son_iguales() {
        Merchant a = new Merchant("M-001");
        Merchant b = new Merchant("M-002");
        assertNotEquals(a.id(), b.id());
    }
}
