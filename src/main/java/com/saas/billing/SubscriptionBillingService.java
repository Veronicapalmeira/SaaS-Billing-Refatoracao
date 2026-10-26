
package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "O repositório de clientes não pode ser nulo");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "O gateway de pagamento não pode ser nulo");
        this.notificationService = Objects.requireNonNull(notificationService, "O serviço de notificação não pode ser nulo");
        this.clock = Objects.requireNonNull(clock, "O relógio não pode ser nulo");
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        String customerId = command.customerId();
        String planType = command.planType();
        double basePrice = command.basePrice().amount().doubleValue();
        String signupDateStr = command.signupDateStr();
        int overdueDays = command.overdueDays();
        String promoCode = command.promoCode();
        boolean isNonProfit = command.isNonProfit();
        String paymentCardToken = command.paymentCardToken();

        List<String> logs = new ArrayList<>();

        LocalDate signupDate = LocalDate.parse(signupDateStr);
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15);
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10);
        }

        if (isNonProfit) {
            calculatedPrice = calculatedPrice * 0.70;
        }

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY")
                    && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (promoCode.equalsIgnoreCase("WELCOME10")
                    && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (overdueDays > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * overdueDays;
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }

        Optional<Customer> customer = customerRepository.findById(customerId);

        if (customer.isPresent()) {
            String email = customer.get().email();

            if (!customer.get().isActive()) {
                logs.add("Cliente inativo. Cobrança cancelada: " + customerId);
                return logs;
            }

            Money amount = Money.of(calculatedPrice);
            boolean paymentSuccess = paymentGateway.charge(paymentCardToken, amount);

            if (paymentSuccess) {
                logs.add("Pagamento processado com sucesso: R$ " + calculatedPrice);
            } else {
                logs.add("Falha no pagamento do cartão para: " + customerId);
            }

            customerRepository.saveInvoice(customerId, amount, paymentSuccess ? InvoiceStatus.PAID : InvoiceStatus.FAILED);

            notificationService.sendInvoiceReceipt(email, amount);

            logs.add("E-mail disparado para: " + email);
        }

        return logs;
    }
}
