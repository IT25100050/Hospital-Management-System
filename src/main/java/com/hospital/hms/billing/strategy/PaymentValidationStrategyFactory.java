package com.hospital.hms.billing.strategy;

import com.hospital.hms.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory pattern: hides the mapping of "payment method name" -> concrete Strategy
 * implementation from the rest of the application. PaymentServiceImpl just asks this
 * factory for a strategy by name and calls it; it never instantiates a strategy itself.
 */
@Component
public class PaymentValidationStrategyFactory {

    private final Map<String, PaymentValidationStrategy> strategies;

    public PaymentValidationStrategyFactory(CashPaymentValidationStrategy cash,
                                             CreditCardPaymentValidationStrategy creditCard,
                                             InsurancePaymentValidationStrategy insurance) {
        this.strategies = Map.of(
                "CASH", cash,
                "CREDIT_CARD", creditCard,
                "INSURANCE", insurance
        );
    }

    public PaymentValidationStrategy getStrategy(String paymentMethod) {
        PaymentValidationStrategy strategy = strategies.get(paymentMethod == null ? null : paymentMethod.toUpperCase());
        if (strategy == null) {
            throw new BadRequestException("Unsupported payment method: " + paymentMethod +
                    ". Supported methods are: " + strategies.keySet());
        }
        return strategy;
    }
}
