package com.hospital.hms.audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    // Which part of the system this happened in, e.g. "Billing", "Pharmacy" — derived
    // from the service class name so the audit trail reads clearly in the UI.
    private String module;

    // A human-readable verb, e.g. "CREATE", "UPDATE", "DELETE", "VIEW" — derived from the
    // method name (createX / updateX / deleteX / getX...) rather than the raw method name.
    private String action;

    private String details;
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String username, String module, String action, String details, LocalDateTime timestamp) {
        this.username = username;
        this.module = module;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
