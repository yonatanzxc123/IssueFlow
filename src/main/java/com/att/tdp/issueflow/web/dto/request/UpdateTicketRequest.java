package com.att.tdp.issueflow.web.dto.request;

import com.att.tdp.issueflow.domain.enums.TicketPriority;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateTicketRequest(
        @Size(max = 255) String title,
        @Size(max = 10000) String description,
        TicketStatus status,
        TicketPriority priority,
        Long assigneeId,
        Instant dueDate
) {
}
