package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.AuditLog;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.web.dto.response.AuditLogResponse;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        User performedBy = auditLog.getPerformedBy();
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                performedBy == null ? null : performedBy.getId(),
                auditLog.getActor(),
                auditLog.getTimestamp()
        );
    }
}
