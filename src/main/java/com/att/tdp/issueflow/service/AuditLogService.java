package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.AuditLog;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.ActorType;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordProjectAction(AuditAction action, Long projectId) {
        recordAction(action, AuditEntityType.PROJECT, projectId);
    }

    @Transactional
    public void recordTicketAction(AuditAction action, Long ticketId) {
        recordAction(action, AuditEntityType.TICKET, ticketId);
    }

    private void recordAction(AuditAction action, AuditEntityType entityType, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActor(ActorType.USER);
        auditLog.setPerformedBy(currentUserOrNull());
        auditLogRepository.save(auditLog);
    }

    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }
}
