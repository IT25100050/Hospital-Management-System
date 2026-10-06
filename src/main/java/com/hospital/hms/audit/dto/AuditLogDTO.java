package com.hospital.hms.audit.dto;

import java.time.LocalDateTime;

public class AuditLogDTO {
    private Long id;
    private String username;
    private String module;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public AuditLogDTO(Long id, String username, String module, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.username = username;
        this.module = module;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
