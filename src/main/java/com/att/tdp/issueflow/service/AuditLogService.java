package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.AuditLog;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.ActorType;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.security.UserPrincipal;
import com.att.tdp.issueflow.web.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.web.mapper.AuditLogMapper;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public void recordImportAction(Long projectId) {
        recordAction(AuditAction.IMPORT, AuditEntityType.PROJECT, projectId);
    }

    @Transactional
    public void recordPublicUserCreation(Long userId) {
        recordAction(AuditAction.CREATE_USER, AuditEntityType.USER, userId, ActorType.SYSTEM, null);
    }

    @Transactional
    public void recordUserAction(AuditAction action, Long userId) {
        recordAction(action, AuditEntityType.USER, userId, ActorType.USER, currentUserOrNull());
    }

    @Transactional
    public void recordUserDeletion(Long userId) {
        recordAction(AuditAction.DELETE_USER, AuditEntityType.USER, userId, ActorType.USER, currentUserUnless(userId));
    }

    @Transactional
    public void recordLogoutAction(Long userId) {
        recordAction(AuditAction.LOGOUT, AuditEntityType.AUTH, userId, ActorType.USER, currentUserOrNull());
    }

    @Transactional
    public void recordTicketAction(AuditAction action, Long ticketId) {
        recordAction(action, AuditEntityType.TICKET, ticketId, ActorType.USER, currentUserOrNull());
    }

    @Transactional
    public void recordSystemTicketAction(AuditAction action, Long ticketId) {
        recordAction(action, AuditEntityType.TICKET, ticketId, ActorType.SYSTEM, null);
    }

    @Transactional
    public void recordCommentAction(AuditAction action, Long commentId) {
        recordAction(action, AuditEntityType.COMMENT, commentId);
    }

    @Transactional
    public void recordDependencyAction(AuditAction action, Long dependencyId) {
        recordAction(action, AuditEntityType.TICKET_DEPENDENCY, dependencyId);
    }

    @Transactional
    public void recordAttachmentAction(AuditAction action, Long attachmentId) {
        recordAction(action, AuditEntityType.ATTACHMENT, attachmentId);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(
            String entityType,
            Long entityId,
            String action,
            String actor
    ) {
        AuditEntityType resolvedEntityType = parseEnum(entityType, AuditEntityType.class, "entityType");
        AuditAction resolvedAction = parseEnum(action, AuditAction.class, "action");
        ActorType resolvedActor = parseEnum(actor, ActorType.class, "actor");

        return auditLogRepository.findAll(
                        auditLogFilter(resolvedEntityType, entityId, resolvedAction, resolvedActor),
                        Sort.by(Sort.Direction.DESC, "timestamp")
                )
                .stream()
                .map(AuditLogMapper::toResponse)
                .toList();
    }

    private void recordAction(AuditAction action, AuditEntityType entityType, Long entityId) {
        recordAction(action, entityType, entityId, ActorType.USER, currentUserOrNull());
    }

    private void recordAction(
            AuditAction action,
            AuditEntityType entityType,
            Long entityId,
            ActorType actor,
            User performedBy
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActor(actor);
        auditLog.setPerformedBy(performedBy);
        auditLogRepository.save(auditLog);
    }

    private Specification<AuditLog> auditLogFilter(
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            ActorType actor
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();
            if (entityType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("action"), action));
            }
            if (actor != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("actor"), actor));
            }
            return predicate;
        };
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid " + parameterName + " value: " + value);
        }
    }

    private User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }

    private User currentUserUnless(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        if (principal.getId().equals(userId)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }
}
