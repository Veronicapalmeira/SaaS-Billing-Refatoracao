package com.saas.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public class PricingCalculator {

    private static final int LONG_LOYALTY_MONTHS = 24;
    private static final int SHORT_LOYALTY_MONTHS = 12;
    private static final BigDecimal LONG_LOYALTY_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal SHORT_LOYALTY_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_FACTOR = new BigDecimal("0.70");

    private static final String BLACK_FRIDAY_CODE = "BLACKFRIDAY";
    private static final int BLACK_FRIDAY_MONTH = 11;
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of(30.0);
    private static final String WELCOME_CODE = "WELCOME10";
    private static final int WELCOME_MAX_MONTHS = 1;
    private static final BigDecimal WELCOME_FACTOR = new BigDecimal("0.90");

    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    private final Clock clock;

    public PricingCalculator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "O relógio não pode ser nulo");
    }

    public Money calculate(BillingCommand command) {
        LocalDate signupDate = LocalDate.parse(command.signupDateStr());
        LocalDate today = LocalDate.now(clock);
        int monthsActive = monthsBetween(signupDate, today);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.isNonProfit());
        price = applyPromoCode(price, command.promoCode(), today, monthsActive);
        price = applyLateFees(price, command.overdueDays());
        return price.zeroIfNegative();
    }

    private int monthsBetween(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > LONG_LOYALTY_MONTHS) {
            return basePrice.subtract(basePrice.multiply(LONG_LOYALTY_DISCOUNT));
        }
        if (monthsActive > SHORT_LOYALTY_MONTHS) {
            return basePrice.subtract(basePrice.multiply(SHORT_LOYALTY_DISCOUNT));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean isNonProfit) {
        return isNonProfit ? price.multiply(NON_PROFIT_FACTOR) : price;
    }

    private Money applyPromoCode(Money price, String promoCode, LocalDate today, int monthsActive) {
        if (BLACK_FRIDAY_CODE.equalsIgnoreCase(promoCode) && today.getMonthValue() == BLACK_FRIDAY_MONTH) {
            return price.subtract(BLACK_FRIDAY_DISCOUNT);
        }
        if (WELCOME_CODE.equalsIgnoreCase(promoCode) && monthsActive <= WELCOME_MAX_MONTHS) {
            return price.multiply(WELCOME_FACTOR);
        }
        return price;
    }

    private Money applyLateFees(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.multiply(LATE_PENALTY_RATE);
        Money dailyInterest = price.multiply(DAILY_INTEREST_RATE).multiply(BigDecimal.valueOf(overdueDays));
        return price.add(penalty).add(dailyInterest);
    }
}
