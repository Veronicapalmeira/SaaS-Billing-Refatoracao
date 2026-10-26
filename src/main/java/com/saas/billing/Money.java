package com.saas.billing;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "O valor não pode ser nulo");
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public Money zeroIfNegative() {
        return isNegative() ? new Money(BigDecimal.ZERO) : this;
    }
}
