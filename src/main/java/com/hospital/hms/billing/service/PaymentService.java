package com.hospital.hms.billing.service;

import com.hospital.hms.billing.dto.PaymentDTO;
import java.util.List;

public interface PaymentService {
    PaymentDTO processPayment(PaymentDTO dto);
    PaymentDTO getPaymentById(Long id);
    List<PaymentDTO> getPaymentsByBillId(Long billId);
    PaymentDTO updatePayment(Long id, PaymentDTO dto);
    void deletePayment(Long id);
}
