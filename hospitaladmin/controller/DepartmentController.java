package com.hospital.hms.hospitaladmin.controller;

import com.hospital.hms.common.dto.ApiResponse;
import com.hospital.hms.hospitaladmin.dto.DepartmentDTO;
import com.hospital.hms.hospitaladmin.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(@Valid @RequestBody DepartmentDTO dto) {
        DepartmentDTO response = departmentService.createDepartment(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        DepartmentDTO response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllDepartments() {
        List<DepartmentDTO> response = departmentService.getAllDepartments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Departments retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDTO dto) {
        DepartmentDTO response = departmentService.updateDepartment(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department deleted successfully", null));
    }
}
