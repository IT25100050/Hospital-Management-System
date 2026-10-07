package com.hospital.hms.billing.controller;

import com.hospital.hms.billing.dto.BillDTO;
import com.hospital.hms.billing.service.BillService;
import com.hospital.hms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillDTO>> createBill(@Valid @RequestBody BillDTO dto) {
        BillDTO response = billService.createBill(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillDTO>> getBillById(@PathVariable Long id) {
        BillDTO response = billService.getBillById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill retrieved successfully", response));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<ApiResponse<BillDTO>> getBillByInvoiceNumber(@PathVariable String invoiceNumber) {
        BillDTO response = billService.getBillByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillDTO>>> getAllBills() {
        List<BillDTO> response = billService.getAllBills();
        return ResponseEntity.ok(new ApiResponse<>(true, "Bills retrieved successfully", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<BillDTO>>> getBillsByPatient(@PathVariable Long patientId) {
        List<BillDTO> response = billService.getBillsByPatientId(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Patient bills retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BillDTO>> updateBill(@PathVariable Long id, @RequestBody BillDTO dto) {
        BillDTO response = billService.updateBill(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBill(@PathVariable Long id) {
        billService.deleteBill(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bill deleted successfully", null));
    }
}
