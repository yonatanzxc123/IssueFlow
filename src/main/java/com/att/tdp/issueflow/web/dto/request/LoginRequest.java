package com.att.tdp.issueflow.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 255) String password
) {
}
