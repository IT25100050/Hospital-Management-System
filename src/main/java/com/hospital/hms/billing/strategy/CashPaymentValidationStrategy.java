package com.hospital.hms.billing.strategy;

import com.hospital.hms.billing.dto.PaymentDTO;
import org.springframework.stereotype.Component;

@Component
public class CashPaymentValidationStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(PaymentDTO dto) {
        // Cash payments need no external reference — nothing further to validate.
    }
}
