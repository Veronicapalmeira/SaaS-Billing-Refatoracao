package com.saas.billing;

public class CardTokenPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
    }
}
