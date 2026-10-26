package com.saas.billing;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final PricingCalculator pricingCalculator;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "O repositório de clientes não pode ser nulo");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "O gateway de pagamento não pode ser nulo");
        this.notificationService = Objects.requireNonNull(notificationService, "O serviço de notificação não pode ser nulo");
        this.pricingCalculator = new PricingCalculator(clock);
    }

    public List<String> processMonthlyBilling(BillingCommand command) {
        List<String> logs = new ArrayList<>();
        Money price = pricingCalculator.calculate(command);
        Optional<Customer> customer = customerRepository.findById(command.customerId());

        if (customer.isEmpty()) {
            return logs;
        }
        if (!customer.get().isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return logs;
        }

        boolean paid = charge(command, price, logs);
        customerRepository.saveInvoice(command.customerId(), price, paid ? InvoiceStatus.PAID : InvoiceStatus.FAILED);
        sendReceipt(customer.get(), price, logs);
        return logs;
    }

    private boolean charge(BillingCommand command, Money price, List<String> logs) {
        boolean paid = paymentGateway.charge(command.paymentCardToken(), price);
        if (paid) {
            logs.add("Pagamento processado com sucesso: R$ " + price.amount().doubleValue());
        } else {
            logs.add("Falha no pagamento do cartão para: " + command.customerId());
        }
        return paid;
    }

    private void sendReceipt(Customer customer, Money price, List<String> logs) {
        notificationService.sendInvoiceReceipt(customer.email(), price);
        logs.add("E-mail disparado para: " + customer.email());
    }
}
