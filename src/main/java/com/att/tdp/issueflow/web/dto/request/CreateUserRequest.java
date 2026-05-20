package com.att.tdp.issueflow.web.dto.request;

import com.att.tdp.issueflow.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String fullName,
        @NotNull Role role,
        @Size(max = 255) String password
) {
}
