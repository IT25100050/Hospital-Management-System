package com.hospital.hms.audit.aspect;

import com.hospital.hms.audit.model.AuditLog;
import com.hospital.hms.audit.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Cross-cutting audit logging implemented with Spring AOP (the Proxy pattern): every
 * ...ServiceImpl method call in the application is intercepted here, without any of the
 * individual services needing to know an audit trail exists.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning("execution(* com.hospital.hms..*ServiceImpl.*(..)) && !within(com.hospital.hms.audit..*)")
    public void logServiceAction(JoinPoint joinPoint) {
        record(joinPoint, "SUCCESS");
    }

    @AfterThrowing(pointcut = "execution(* com.hospital.hms..*ServiceImpl.*(..)) && !within(com.hospital.hms.audit..*)", throwing = "ex")
    public void logServiceFailure(JoinPoint joinPoint, Exception ex) {
        record(joinPoint, "FAILED: " + ex.getMessage());
    }

    private void record(JoinPoint joinPoint, String outcome) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        String module = className.replace("ServiceImpl", "");
        String action = deriveAction(methodName);

        AuditLog auditLog = new AuditLog(
                currentUsername(),
                module,
                action,
                methodName + " — " + outcome,
                LocalDateTime.now()
        );

        auditLogRepository.save(auditLog);
    }

    private String deriveAction(String methodName) {
        String lower = methodName.toLowerCase();
        if (lower.startsWith("create") || lower.startsWith("add") || lower.startsWith("order")
                || lower.startsWith("process") || lower.startsWith("register")) return "CREATE";
        if (lower.startsWith("update") || lower.startsWith("fulfill") || lower.startsWith("reset")) return "UPDATE";
        if (lower.startsWith("delete") || lower.startsWith("cancel")) return "DELETE";
        if (lower.startsWith("get") || lower.startsWith("find") || lower.startsWith("list")) return "VIEW";
        return "OTHER";
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
