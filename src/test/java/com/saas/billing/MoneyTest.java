
package com.saas.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void deveSomarDoisValores() {
        Money primeiro = Money.of(10.50);
        Money segundo = Money.of(5.25);

        Money resultado = primeiro.add(segundo);

        assertEquals(0, new BigDecimal("15.75").compareTo(resultado.amount()));
    }

    @Test
    void deveSubtrairDoisValores() {
        Money primeiro = Money.of(10.00);
        Money segundo = Money.of(3.50);

        Money resultado = primeiro.subtract(segundo);

        assertEquals(0, new BigDecimal("6.50").compareTo(resultado.amount()));
    }

    @Test
    void deveMultiplicarPorUmFator() {
        Money valor = Money.of(100.00);

        Money resultado = valor.multiply(new BigDecimal("0.70"));

        assertEquals(0, new BigDecimal("70.000").compareTo(resultado.amount()));
    }

    @Test
    void deveZerarValorNegativo() {
        Money valor = Money.of(-5.00);

        Money resultado = valor.zeroIfNegative();

        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.amount()));
    }
}
