package com.saas.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTokenPaymentGatewayTest {

    private final CardTokenPaymentGateway gateway = new CardTokenPaymentGateway();

    @Test
    void deveAprovarTokenComPrefixoValido() {
        assertTrue(gateway.charge("tok_123", Money.of(10.0)));
    }

    @Test
    void deveRecusarTokenSemPrefixoValido() {
        assertFalse(gateway.charge("card_123", Money.of(10.0)));
    }

    @Test
    void deveRecusarTokenNulo() {
        assertFalse(gateway.charge(null, Money.of(10.0)));
    }
}
