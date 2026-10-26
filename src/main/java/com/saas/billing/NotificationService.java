package com.saas.billing;

public interface NotificationService {

    void sendInvoiceReceipt(String recipientEmail, Money amount);
}
