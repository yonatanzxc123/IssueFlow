package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.domain.TicketDependency;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

    List<TicketDependency> findByTicketIdOrderByBlockerTicketIdAsc(Long ticketId);

    Optional<TicketDependency> findByTicketIdAndBlockerTicketId(Long ticketId, Long blockerTicketId);

    boolean existsByTicketIdAndBlockerTicketStatusNot(Long ticketId, TicketStatus status);
}
