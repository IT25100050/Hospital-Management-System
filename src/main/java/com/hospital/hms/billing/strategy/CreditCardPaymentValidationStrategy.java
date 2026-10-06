package com.hospital.hms.billing.strategy;

import com.hospital.hms.billing.dto.PaymentDTO;
import com.hospital.hms.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreditCardPaymentValidationStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(PaymentDTO dto) {
        if (!StringUtils.hasText(dto.getTransactionReference())) {
            throw new BadRequestException("A transaction reference is required for credit card payments.");
        }
    }
}
