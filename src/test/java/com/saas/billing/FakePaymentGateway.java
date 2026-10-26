package com.saas.billing;

import java.util.ArrayList;
import java.util.List;

class FakePaymentGateway implements PaymentGateway {

    record Charge(String cardToken, Money amount) {
    }

    private final boolean approves;
    private final List<Charge> charges = new ArrayList<>();

    private FakePaymentGateway(boolean approves) {
        this.approves = approves;
    }

    static FakePaymentGateway approving() {
        return new FakePaymentGateway(true);
    }

    static FakePaymentGateway declining() {
        return new FakePaymentGateway(false);
    }

    List<Charge> charges() {
        return charges;
    }

    @Override
    public boolean charge(String cardToken, Money amount) {
        charges.add(new Charge(cardToken, amount));
        return approves;
    }
}
