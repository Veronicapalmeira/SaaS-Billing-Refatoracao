package com.saas.billing;

import java.util.ArrayList;
import java.util.List;

class InMemoryNotificationService implements NotificationService {

    record Receipt(String recipientEmail, Money amount) {
    }

    private final List<Receipt> receipts = new ArrayList<>();
    private boolean failing;

    void simulateFailure() {
        failing = true;
    }

    List<Receipt> receipts() {
        return receipts;
    }

    @Override
    public void sendInvoiceReceipt(String recipientEmail, Money amount) {
        if (failing) {
            throw new NotificationException("Falha simulada no envio para: " + recipientEmail, null);
        }
        receipts.add(new Receipt(recipientEmail, amount));
    }
}
