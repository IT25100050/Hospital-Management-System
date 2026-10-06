package com.hospital.hms.audit.controller;

import com.hospital.hms.audit.dto.AuditLogDTO;
import com.hospital.hms.audit.service.AuditLogService;
import com.hospital.hms.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only access to the audit trail. Restricted to ADMIN in SecurityConfig, since every
 * create/update/delete across every module is recorded here by AuditAspect.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<AuditLogDTO> response = auditLogService.getRecentLogs(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs retrieved successfully", response));
    }

    @GetMapping("/logs/user/{username}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getLogsByUsername(@PathVariable String username) {
        List<AuditLogDTO> response = auditLogService.getLogsByUsername(username);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs retrieved successfully", response));
    }

    @GetMapping("/logs/module/{module}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getLogsByModule(@PathVariable String module) {
        List<AuditLogDTO> response = auditLogService.getLogsByModule(module);
        return ResponseEntity.ok(new ApiResponse<>(true, "Audit logs retrieved successfully", response));
    }
}
