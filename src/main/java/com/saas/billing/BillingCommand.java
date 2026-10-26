package com.saas.billing;

public record BillingCommand(
        String customerId,
        String planType,
        Money basePrice,
        String signupDateStr,
        int overdueDays,
        String promoCode,
        boolean isNonProfit,
        String paymentCardToken
) {
}
