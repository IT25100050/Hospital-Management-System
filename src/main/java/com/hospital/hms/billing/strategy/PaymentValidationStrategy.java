package com.hospital.hms.billing.strategy;

import com.hospital.hms.billing.dto.PaymentDTO;

/**
 * Strategy pattern: each payment method has its own rules for what counts as a valid
 * payment. PaymentServiceImpl depends only on this interface, not on any concrete method,
 * so a new payment method can be added later (e.g. ONLINE_TRANSFER) without touching
 * PaymentServiceImpl at all.
 */
public interface PaymentValidationStrategy {
    void validate(PaymentDTO dto);
}
