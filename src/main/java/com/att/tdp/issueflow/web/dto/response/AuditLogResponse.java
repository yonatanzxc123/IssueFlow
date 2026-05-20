package com.att.tdp.issueflow.web.dto.response;

import com.att.tdp.issueflow.domain.enums.ActorType;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        AuditEntityType entityType,
        Long entityId,
        Long performedBy,
        ActorType actor,
        Instant timestamp
) {
}
