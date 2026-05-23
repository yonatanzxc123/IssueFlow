package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.TicketPriority;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketEscalationService {

    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public TicketEscalationService(
            TicketRepository ticketRepository,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${app.escalation.fixed-delay-ms:60000}",
            initialDelayString = "${app.escalation.initial-delay-ms:60000}"
    )
    public void scheduledEscalation() {
        escalateOverdueTickets();
    }

    @Transactional
    public int escalateOverdueTickets() {
        int changedCount = 0;
        for (Ticket ticket : ticketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalse(
                Instant.now(clock),
                TicketStatus.DONE
        )) {
            if (escalate(ticket)) {
                changedCount++;
                auditLogService.recordSystemTicketAction(AuditAction.AUTO_ESCALATE, ticket.getId());
            }
        }
        return changedCount;
    }

    private boolean escalate(Ticket ticket) {
        boolean changed = false;
        switch (ticket.getPriority()) {
            case LOW -> {
                ticket.setPriority(TicketPriority.MEDIUM);
                changed = true;
            }
            case MEDIUM -> {
                ticket.setPriority(TicketPriority.HIGH);
                changed = true;
            }
            case HIGH -> {
                ticket.setPriority(TicketPriority.CRITICAL);
                changed = true;
            }
            case CRITICAL -> {
            }
        }

        if (ticket.getPriority() == TicketPriority.CRITICAL && !ticket.isOverdue()) {
            ticket.setOverdue(true);
            changed = true;
        }
        return changed;
    }
}
