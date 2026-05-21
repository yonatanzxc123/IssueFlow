package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.TicketDependency;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.web.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.web.dto.response.DependencyResponse;
import com.att.tdp.issueflow.web.mapper.TicketMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketDependencyService {

    private final TicketDependencyRepository ticketDependencyRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public TicketDependencyService(
            TicketDependencyRepository ticketDependencyRepository,
            TicketRepository ticketRepository,
            AuditLogService auditLogService
    ) {
        this.ticketDependencyRepository = ticketDependencyRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DependencyResponse addDependency(Long ticketId, AddDependencyRequest request) {
        Ticket ticket = findActiveTicket(ticketId);
        Ticket blocker = findActiveTicket(request.blockedBy());
        validateDependency(ticket, blocker);

        if (ticketDependencyRepository.findByTicketIdAndBlockerTicketId(ticketId, blocker.getId()).isPresent()) {
            throw new ConflictException("Ticket dependency already exists");
        }

        TicketDependency dependency = new TicketDependency();
        dependency.setTicket(ticket);
        dependency.setBlockerTicket(blocker);

        TicketDependency saved = ticketDependencyRepository.save(dependency);
        auditLogService.recordDependencyAction(AuditAction.ADD_DEPENDENCY, saved.getId());
        return TicketMapper.toDependencyResponse(blocker);
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependencies(Long ticketId) {
        findActiveTicket(ticketId);
        return ticketDependencyRepository.findByTicketIdOrderByBlockerTicketIdAsc(ticketId)
                .stream()
                .filter(dependency -> !dependency.getBlockerTicket().isDeleted())
                .map(TicketDependency::getBlockerTicket)
                .map(TicketMapper::toDependencyResponse)
                .toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        findActiveTicket(ticketId);
        findActiveTicket(blockerId);

        TicketDependency dependency = ticketDependencyRepository.findByTicketIdAndBlockerTicketId(ticketId, blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket dependency not found"));

        ticketDependencyRepository.delete(dependency);
        auditLogService.recordDependencyAction(AuditAction.REMOVE_DEPENDENCY, dependency.getId());
    }

    private Ticket findActiveTicket(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private void validateDependency(Ticket ticket, Ticket blocker) {
        if (ticket.getId().equals(blocker.getId())) {
            throw new ConflictException("Ticket cannot depend on itself");
        }
        if (!ticket.getProject().getId().equals(blocker.getProject().getId())) {
            throw new ConflictException("Dependent tickets must belong to the same project");
        }
    }
}
