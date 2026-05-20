package com.att.tdp.issueflow.web.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 200) String name,
        @Size(max = 5000) String description
) {
}
