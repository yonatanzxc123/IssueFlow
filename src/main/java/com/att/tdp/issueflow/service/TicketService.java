package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Project;
import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.domain.enums.TicketPriority;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.web.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.web.dto.response.TicketResponse;
import com.att.tdp.issueflow.web.mapper.TicketMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public TicketService(
            TicketRepository ticketRepository,
            ProjectRepository projectRepository,
            UserService userService,
            AuditLogService auditLogService
    ) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByProject(Long projectId) {
        findActiveProject(projectId);
        return ticketRepository.findByProjectIdAndDeletedFalseOrderByIdAsc(projectId)
                .stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId) {
        return TicketMapper.toResponse(findActiveTicket(ticketId));
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Project project = findActiveProject(request.projectId());
        User assignee = findAssigneeOrNull(request.assigneeId());

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(request.status());
        ticket.setPriority(request.priority());
        ticket.setType(request.type());
        ticket.setProject(project);
        ticket.setAssignee(assignee);
        ticket.setDueDate(request.dueDate());

        Ticket saved = ticketRepository.save(ticket);
        auditLogService.recordTicketAction(AuditAction.CREATE_TICKET, saved.getId());
        return TicketMapper.toResponse(saved);
    }

    @Transactional
    public void updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = findActiveTicket(ticketId);
        ensureTicketCanBeUpdated(ticket);

        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.status() != null) {
            validateStatusTransition(ticket.getStatus(), request.status());
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            updatePriority(ticket, request.priority());
        }
        if (request.assigneeId() != null) {
            ticket.setAssignee(userService.findUser(request.assigneeId()));
        }
        if (request.dueDate() != null) {
            ticket.setDueDate(request.dueDate());
        }

        auditLogService.recordTicketAction(AuditAction.UPDATE_TICKET, ticket.getId());
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = findActiveTicket(ticketId);
        ticket.setDeleted(true);
        ticket.setDeletedAt(Instant.now());
        auditLogService.recordTicketAction(AuditAction.DELETE_TICKET, ticket.getId());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getDeletedTickets(Long projectId) {
        findActiveProject(projectId);
        return ticketRepository.findByProjectIdAndDeletedTrueOrderByDeletedAtDesc(projectId)
                .stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Transactional
    public TicketResponse restoreTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        ensureProjectActive(ticket.getProject());
        if (ticket.isDeleted()) {
            ticket.setDeleted(false);
            ticket.setDeletedAt(null);
            auditLogService.recordTicketAction(AuditAction.RESTORE_TICKET, ticket.getId());
        }
        return TicketMapper.toResponse(ticket);
    }

    private Ticket findActiveTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        ensureProjectActive(ticket.getProject());
        return ticket;
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private User findAssigneeOrNull(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userService.findUser(assigneeId);
    }

    private void ensureProjectActive(Project project) {
        if (project.isDeleted()) {
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private void ensureTicketCanBeUpdated(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new ConflictException("Ticket cannot be updated once it is DONE");
        }
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus requestedStatus) {
        if (lifecycleRank(requestedStatus) < lifecycleRank(currentStatus)) {
            throw new ConflictException("Ticket status cannot move backward");
        }
    }

    private int lifecycleRank(TicketStatus status) {
        return switch (status) {
            case TODO -> 0;
            case IN_PROGRESS -> 1;
            case IN_REVIEW -> 2;
            case DONE -> 3;
        };
    }

    private void updatePriority(Ticket ticket, TicketPriority requestedPriority) {
        if (requestedPriority != ticket.getPriority()) {
            ticket.setPriority(requestedPriority);
            ticket.setOverdue(false);
        }
    }
}
