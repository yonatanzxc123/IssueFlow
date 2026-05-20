package com.att.tdp.issueflow.exception;

public record FieldValidationError(
        String field,
        String message
) {
}
