package com.saas.billing;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class SmtpNotificationService implements NotificationService {

    private static final String SMTP_HOST = "smtp.empresa.com";
    private static final int SMTP_PORT = 587;
    private static final String SENDER = "billing@empresa.com";

    @Override
    public void sendInvoiceReceipt(String recipientEmail, Money amount) {
        try {
            SimpleEmail emailSender = new SimpleEmail();
            emailSender.setHostName(SMTP_HOST);
            emailSender.setSmtpPort(SMTP_PORT);
            emailSender.setFrom(SENDER);
            emailSender.setSubject("Sua Fatura Mensal");
            emailSender.setMsg("Olá! Sua fatura foi processada no valor de R$ " + amount.amount().doubleValue());
            emailSender.addTo(recipientEmail);

            // emailSender.send();
        } catch (EmailException e) {
            throw new NotificationException("Erro ao preparar o e-mail para: " + recipientEmail, e);
        }
    }
}
