package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.web.dto.response.DependencyResponse;
import com.att.tdp.issueflow.web.dto.response.TicketResponse;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketResponse toResponse(Ticket ticket) {
        User assignee = ticket.getAssignee();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getType(),
                ticket.getProject().getId(),
                assignee == null ? null : assignee.getId(),
                ticket.getDueDate(),
                ticket.isOverdue()
        );
    }

    public static DependencyResponse toDependencyResponse(Ticket blocker) {
        return new DependencyResponse(
                blocker.getId(),
                blocker.getTitle(),
                blocker.getStatus()
        );
    }
}
