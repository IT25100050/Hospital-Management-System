package com.hospital.hms.billing.controller;

import com.hospital.hms.billing.dto.PaymentDTO;
import com.hospital.hms.billing.service.PaymentService;
import com.hospital.hms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDTO>> processPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        PaymentDTO response = paymentService.processPayment(paymentDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment processed successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(@PathVariable Long id) {
        PaymentDTO response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment retrieved successfully", response));
    }

    @GetMapping("/bill/{billId}")
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getPaymentsForBill(@PathVariable Long billId) {
        List<PaymentDTO> response = paymentService.getPaymentsByBillId(billId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill payments retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable Long id, @RequestBody PaymentDTO dto) {
        PaymentDTO response = paymentService.updatePayment(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment deleted and bill balance adjusted successfully", null));
    }
}
