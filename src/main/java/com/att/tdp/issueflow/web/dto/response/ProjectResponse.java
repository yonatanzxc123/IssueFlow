package com.att.tdp.issueflow.web.dto.response;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId
) {
}
