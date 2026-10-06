package com.hospital.hms.pharmacy.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.pharmacy.dto.MedicineDTO;
import com.hospital.hms.pharmacy.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicineDTO>> addMedicine(@Valid @RequestBody MedicineDTO dto) {
        MedicineDTO response = medicineService.addMedicine(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicine added to inventory successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineDTO>> getMedicineById(@PathVariable Long id) {
        MedicineDTO response = medicineService.getMedicineById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicine retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineDTO>>> getAllMedicines() {
        List<MedicineDTO> response = medicineService.getAllMedicines();
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicines retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicineDTO>> updateMedicine(@PathVariable Long id, @RequestBody MedicineDTO dto) {
        MedicineDTO response = medicineService.updateMedicine(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicine details updated successfully", response));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<MedicineDTO>> updateStockQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        MedicineDTO response = medicineService.updateStockQuantity(id, quantity);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicine stock quantity updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Medicine removed from inventory successfully", null));
    }
}
