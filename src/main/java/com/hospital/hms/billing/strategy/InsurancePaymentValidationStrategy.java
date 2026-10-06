package com.hospital.hms.billing.strategy;

import com.hospital.hms.billing.dto.PaymentDTO;
import com.hospital.hms.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InsurancePaymentValidationStrategy implements PaymentValidationStrategy {
    @Override
    public void validate(PaymentDTO dto) {
        if (!StringUtils.hasText(dto.getTransactionReference())) {
            throw new BadRequestException("An insurance claim/policy reference is required for insurance payments.");
        }
    }
}
