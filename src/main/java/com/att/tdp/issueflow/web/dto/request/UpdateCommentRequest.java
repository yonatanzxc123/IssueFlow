package com.att.tdp.issueflow.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank @Size(max = 10000) String content
) {
}
