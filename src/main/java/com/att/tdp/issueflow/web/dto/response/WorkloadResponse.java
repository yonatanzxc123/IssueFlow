package com.att.tdp.issueflow.web.dto.response;

public record WorkloadResponse(
        Long userId,
        String username,
        long openTicketCount
) {
}
