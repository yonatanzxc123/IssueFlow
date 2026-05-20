package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.AuditLog;
import com.att.tdp.issueflow.domain.enums.ActorType;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.AuditEntityType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditEntityType entityType, Long entityId);

    List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);

    List<AuditLog> findByActorOrderByTimestampDesc(ActorType actor);
}
