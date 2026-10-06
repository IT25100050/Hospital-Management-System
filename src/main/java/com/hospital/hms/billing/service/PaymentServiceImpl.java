package com.hospital.hms.billing.service;

import com.hospital.hms.billing.dto.PaymentDTO;
import com.hospital.hms.billing.model.Bill;
import com.hospital.hms.billing.model.Payment;
import com.hospital.hms.billing.repository.BillRepository;
import com.hospital.hms.billing.repository.PaymentRepository;
import com.hospital.hms.billing.strategy.PaymentValidationStrategyFactory;
import com.hospital.hms.common.enums.PaymentStatus;
import com.hospital.hms.common.exception.BadRequestException;
import com.hospital.hms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentValidationStrategyFactory paymentValidationStrategyFactory;

    @Override
    @Transactional
    public PaymentDTO processPayment(PaymentDTO dto) {
        paymentValidationStrategyFactory.getStrategy(dto.getPaymentMethod()).validate(dto);

        Bill bill = billRepository.findById(dto.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + dto.getBillId()));

        if (bill.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Bill is already fully paid.");
        }

        BigDecimal newPaidAmount = bill.getPaidAmount().add(dto.getAmount());
        if (newPaidAmount.compareTo(bill.getTotalAmount()) > 0) {
            throw new BadRequestException("Payment amount exceeds the remaining bill total.");
        }

        bill.setPaidAmount(newPaidAmount);
        bill.setStatus(resolveStatus(newPaidAmount, bill.getTotalAmount()));
        billRepository.save(bill);

        Payment payment = Payment.builder()
                .bill(bill)
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .transactionReference(dto.getTransactionReference())
                .paymentDate(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToDTO(savedPayment);
    }

    @Override
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToDTO(payment);
    }

    @Override
    public List<PaymentDTO> getPaymentsByBillId(Long billId) {
        return paymentRepository.findByBillId(billId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO dto) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        String effectiveMethod = dto.getPaymentMethod() != null ? dto.getPaymentMethod() : payment.getPaymentMethod();
        PaymentDTO effectiveDto = PaymentDTO.builder()
                .paymentMethod(effectiveMethod)
                .transactionReference(dto.getTransactionReference() != null ? dto.getTransactionReference() : payment.getTransactionReference())
                .build();
        paymentValidationStrategyFactory.getStrategy(effectiveMethod).validate(effectiveDto);

        Bill bill = payment.getBill();
        BigDecimal newAmount = dto.getAmount() != null ? dto.getAmount() : payment.getAmount();

        BigDecimal paidWithoutThis = bill.getPaidAmount().subtract(payment.getAmount());
        BigDecimal recalculatedPaid = paidWithoutThis.add(newAmount);

        if (recalculatedPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Payment amount cannot make the paid total negative.");
        }
        if (recalculatedPaid.compareTo(bill.getTotalAmount()) > 0) {
            throw new BadRequestException("Payment amount exceeds the remaining bill total.");
        }

        payment.setAmount(newAmount);
        if (dto.getPaymentMethod() != null) {
            payment.setPaymentMethod(dto.getPaymentMethod());
        }
        if (dto.getTransactionReference() != null) {
            payment.setTransactionReference(dto.getTransactionReference());
        }

        bill.setPaidAmount(recalculatedPaid);
        bill.setStatus(resolveStatus(recalculatedPaid, bill.getTotalAmount()));

        billRepository.save(bill);
        Payment updated = paymentRepository.save(payment);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));

        Bill bill = payment.getBill();
        BigDecimal recalculatedPaid = bill.getPaidAmount().subtract(payment.getAmount());
        if (recalculatedPaid.compareTo(BigDecimal.ZERO) < 0) {
            recalculatedPaid = BigDecimal.ZERO;
        }

        bill.setPaidAmount(recalculatedPaid);
        bill.setStatus(resolveStatus(recalculatedPaid, bill.getTotalAmount()));
        billRepository.save(bill);

        paymentRepository.delete(payment);
    }

    private PaymentStatus resolveStatus(BigDecimal paidAmount, BigDecimal totalAmount) {
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.PENDING;
        } else if (paidAmount.compareTo(totalAmount) >= 0) {
            return PaymentStatus.PAID;
        } else {
            return PaymentStatus.PARTIAL;
        }
    }

    private PaymentDTO mapToDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}
