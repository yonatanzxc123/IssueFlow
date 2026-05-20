package com.att.tdp.issueflow.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull Long authorId,
        @NotBlank @Size(max = 10000) String content
) {
}
