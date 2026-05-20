package com.att.tdp.issueflow.web.dto.request;

import com.att.tdp.issueflow.domain.enums.TicketPriority;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import com.att.tdp.issueflow.domain.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTicketRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @NotNull TicketStatus status,
        @NotNull TicketPriority priority,
        @NotNull TicketType type,
        @NotNull Long projectId,
        Long assigneeId,
        Instant dueDate
) {
}
