package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectIdAndDeletedFalseOrderByIdAsc(Long projectId);

    List<Ticket> findByProjectIdAndDeletedTrueOrderByDeletedAtDesc(Long projectId);

    Optional<Ticket> findByIdAndDeletedFalse(Long id);

    long countByProjectIdAndAssigneeIdAndStatusNotAndDeletedFalse(
            Long projectId,
            Long assigneeId,
            TicketStatus status
    );

    List<Ticket> findByDueDateBeforeAndStatusNotAndDeletedFalse(Instant dueDate, TicketStatus status);
}
