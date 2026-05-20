package com.att.tdp.issueflow.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddDependencyRequest(
        @NotNull Long blockedBy
) {
}
