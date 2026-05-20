package com.att.tdp.issueflow.web.dto.response;

public record AttachmentResponse(
        Long id,
        Long ticketId,
        String filename,
        String contentType,
        Long sizeBytes
) {
}
