package com.saas.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionBillingServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);

    private InMemoryCustomerRepository customerRepository;
    private InMemoryNotificationService notificationService;

    @BeforeEach
    void setUp() {
        customerRepository = new InMemoryCustomerRepository();
        notificationService = new InMemoryNotificationService();
        customerRepository.add(new Customer("cus_1", "ana@cliente.com", "ACTIVE"));
    }

    private SubscriptionBillingService serviceWith(PaymentGateway paymentGateway) {
        return new SubscriptionBillingService(customerRepository, paymentGateway, notificationService, FIXED_CLOCK);
    }

    private static BillingCommand commandFor(String customerId, boolean isNonProfit) {
        return new BillingCommand(customerId, Money.of(100.0), "2026-06-01", 0, null, isNonProfit, "tok_123");
    }

    private static void assertMoney(String expected, Money actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.amount()));
    }

    @Test
    void deveRegistrarFaturaPagaQuandoCobrancaAprovada() {
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = serviceWith(gateway).processMonthlyBilling(commandFor("cus_1", false));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100.0",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(1, gateway.charges().size());
        assertEquals("tok_123", gateway.charges().get(0).cardToken());
        assertEquals(1, customerRepository.invoices().size());
        assertEquals(InvoiceStatus.PAID, customerRepository.invoices().get(0).status());
        assertMoney("100.0", customerRepository.invoices().get(0).amount());
    }

    @Test
    void deveRegistrarFaturaComFalhaQuandoCobrancaRecusada() {
        List<String> logs = serviceWith(FakePaymentGateway.declining())
                .processMonthlyBilling(commandFor("cus_1", false));

        assertEquals(List.of(
                "Falha no pagamento do cartão para: cus_1",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(InvoiceStatus.FAILED, customerRepository.invoices().get(0).status());
    }

    @Test
    void naoDeveCobrarNemNotificarClienteInexistente() {
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = serviceWith(gateway).processMonthlyBilling(commandFor("cus_inexistente", false));

        assertTrue(logs.isEmpty());
        assertTrue(gateway.charges().isEmpty());
        assertTrue(customerRepository.invoices().isEmpty());
        assertTrue(notificationService.receipts().isEmpty());
    }

    @Test
    void deveCancelarCobrancaDeClienteInativo() {
        customerRepository.add(new Customer("cus_2", "bia@cliente.com", "SUSPENDED"));
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = serviceWith(gateway).processMonthlyBilling(commandFor("cus_2", false));

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: cus_2"), logs);
        assertTrue(gateway.charges().isEmpty());
        assertTrue(customerRepository.invoices().isEmpty());
        assertTrue(notificationService.receipts().isEmpty());
    }

    @Test
    void deveEnviarReciboComValorCalculado() {
        List<String> logs = serviceWith(FakePaymentGateway.approving())
                .processMonthlyBilling(commandFor("cus_1", true));

        assertEquals(1, notificationService.receipts().size());
        assertEquals("ana@cliente.com", notificationService.receipts().get(0).recipientEmail());
        assertMoney("70.0", notificationService.receipts().get(0).amount());
        assertTrue(logs.contains("E-mail disparado para: ana@cliente.com"));
    }

    @Test
    void devePropagarFalhaNoEnvioDoRecibo() {
        notificationService.simulateFailure();
        SubscriptionBillingService service = serviceWith(FakePaymentGateway.approving());
        BillingCommand command = commandFor("cus_1", false);

        assertThrows(NotificationException.class, () -> service.processMonthlyBilling(command));
    }
}
