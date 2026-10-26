package com.saas.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {

    private static final String TODAY = "2026-06-15";

    private static PricingCalculator calculatorOn(String isoDate) {
        Instant instant = LocalDate.parse(isoDate).atTime(12, 0).toInstant(ZoneOffset.UTC);
        return new PricingCalculator(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static BillingCommand command(double basePrice, String signupDate, int overdueDays,
                                          String promoCode, boolean isNonProfit) {
        return new BillingCommand("cus_1", Money.of(basePrice), signupDate, overdueDays,
                promoCode, isNonProfit, "tok_123");
    }

    private static void assertMoney(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.amount()),
                () -> "esperado " + expected + " mas foi " + actual.amount());
    }

    @ParameterizedTest(name = "adesão em {0} → R$ {1}")
    @CsvSource({
            "2026-06-15, 100.0",
            "2025-06-15, 100.0",
            "2025-05-15, 90.0",
            "2024-06-15, 90.0",
            "2024-05-15, 85.0",
            "2016-01-01, 85.0"
    })
    void deveAplicarDescontoDeFidelidadePorFaixaDeMeses(String signupDate, String expected) {
        Money price = calculatorOn(TODAY).calculate(command(100.0, signupDate, 0, null, false));

        assertMoney(expected, price);
    }

    @Test
    void deveContarMesesPeloCalendarioIgnorandoODia() {
        Money price = calculatorOn("2026-06-01").calculate(command(100.0, "2025-05-31", 0, null, false));

        assertMoney("90.0", price);
    }

    @Test
    void deveContarMesesCorretamenteNaViradaDoAno() {
        Money price = calculatorOn("2026-01-01").calculate(command(100.0, "2024-12-31", 0, null, false));

        assertMoney("90.0", price);
    }

    @Test
    void deveAplicarDescontoDeOng() {
        Money price = calculatorOn(TODAY).calculate(command(100.0, TODAY, 0, null, true));

        assertMoney("70.0", price);
    }

    @Test
    void deveCombinarFidelidadeEOngSemErroDePontoFlutuante() {
        Money price = calculatorOn(TODAY).calculate(command(100.0, "2024-01-15", 0, null, true));

        assertMoney("59.5", price);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-01", "2026-11-15", "2026-11-30"})
    void deveAplicarBlackFridayEmNovembro(String today) {
        Money price = calculatorOn(today).calculate(command(100.0, "2026-10-01", 0, "BLACKFRIDAY", false));

        assertMoney("70.0", price);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-31", "2026-12-01"})
    void naoDeveAplicarBlackFridayForaDeNovembro(String today) {
        Money price = calculatorOn(today).calculate(command(100.0, "2026-10-01", 0, "BLACKFRIDAY", false));

        assertMoney("100.0", price);
    }

    @Test
    void deveConsiderarOFusoDoRelogioParaDefinirOMes() {
        Instant instant = Instant.parse("2026-11-01T02:00:00Z");
        Clock saoPaulo = Clock.fixed(instant, ZoneId.of("America/Sao_Paulo"));

        Money price = new PricingCalculator(saoPaulo)
                .calculate(command(100.0, "2026-10-01", 0, "BLACKFRIDAY", false));

        assertMoney("100.0", price);
    }

    @Test
    void deveAceitarCupomSemDiferenciarMaiusculas() {
        Money price = calculatorOn("2026-11-20").calculate(command(100.0, "2026-10-01", 0, "blackFriday", false));

        assertMoney("70.0", price);
    }

    @Test
    void deveZerarPrecoQuandoBlackFridaySuperaOValor() {
        Money price = calculatorOn("2026-11-20").calculate(command(20.0, "2026-10-01", 0, "BLACKFRIDAY", false));

        assertMoney("0", price);
    }

    @Test
    void deveZerarPrecoNegativoMesmoComJurosDeAtraso() {
        Money price = calculatorOn("2026-11-20").calculate(command(20.0, "2026-10-01", 10, "BLACKFRIDAY", false));

        assertMoney("0", price);
    }

    @ParameterizedTest(name = "adesão em {0} → R$ {1}")
    @CsvSource({
            "2026-06-15, 90.0",
            "2026-05-15, 90.0",
            "2026-04-15, 100.0"
    })
    void deveAplicarWelcome10SomenteAteUmMesDeAdesao(String signupDate, String expected) {
        Money price = calculatorOn(TODAY).calculate(command(100.0, signupDate, 0, "WELCOME10", false));

        assertMoney(expected, price);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "DESCONTO50", " WELCOME10 "})
    void naoDeveAlterarPrecoComCupomAusenteOuInvalido(String promoCode) {
        Money price = calculatorOn(TODAY).calculate(command(100.0, TODAY, 0, promoCode, false));

        assertMoney("100.0", price);
    }

    @Test
    void deveAplicarMultaEJurosDiariosPorAtraso() {
        Money price = calculatorOn(TODAY).calculate(command(100.0, TODAY, 10, null, false));

        assertMoney("103.0", price);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    void naoDeveCobrarEncargosSemDiasDeAtraso(int overdueDays) {
        Money price = calculatorOn(TODAY).calculate(command(100.0, TODAY, overdueDays, null, false));

        assertMoney("100.0", price);
    }

    @Test
    void naoDeveArredondarEncargosDeAtraso() {
        Money price = calculatorOn(TODAY).calculate(command(99.99, TODAY, 7, null, false));

        assertMoney("102.68973", price);
    }

    @Test
    void deveAplicarTodasAsRegrasNaOrdemDoLegado() {
        Money price = calculatorOn("2026-11-20").calculate(command(200.0, "2024-01-10", 5, "BLACKFRIDAY", true));

        assertMoney("91.225", price);
    }

    @Test
    void deveRejeitarDataDeAdesaoInvalida() {
        PricingCalculator calculator = calculatorOn(TODAY);
        BillingCommand invalid = command(100.0, "15/06/2026", 0, null, false);

        assertThrows(DateTimeParseException.class, () -> calculator.calculate(invalid));
    }
}
