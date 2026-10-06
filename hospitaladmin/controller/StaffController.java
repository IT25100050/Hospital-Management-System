package com.hospital.hms.hospitaladmin.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.hospitaladmin.dto.StaffDTO;
import com.hospital.hms.hospitaladmin.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffDTO>> createStaff(@Valid @RequestBody StaffDTO dto) {
        StaffDTO response = staffService.createStaff(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff member created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffDTO>> getStaffById(@PathVariable Long id) {
        StaffDTO response = staffService.getStaffById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff member retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffDTO>>> getAllStaff() {
        List<StaffDTO> response = staffService.getAllStaff();
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff members retrieved successfully", response));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<StaffDTO>>> getStaffByDepartment(@PathVariable Long departmentId) {
        List<StaffDTO> response = staffService.getStaffByDepartment(departmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department staff retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffDTO>> updateStaff(@PathVariable Long id, @RequestBody StaffDTO dto) {
        StaffDTO response = staffService.updateStaff(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff member updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff member deleted successfully", null));
    }
}
