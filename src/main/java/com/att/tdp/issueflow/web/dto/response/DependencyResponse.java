package com.att.tdp.issueflow.web.dto.response;

import com.att.tdp.issueflow.domain.enums.TicketStatus;

public record DependencyResponse(
        Long id,
        String title,
        TicketStatus status
) {
}
