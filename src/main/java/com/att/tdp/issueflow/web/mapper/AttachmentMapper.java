package com.att.tdp.issueflow.web.mapper;

import com.att.tdp.issueflow.domain.Attachment;
import com.att.tdp.issueflow.web.dto.response.AttachmentResponse;

public final class AttachmentMapper {

    private AttachmentMapper() {
    }

    public static AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTicket().getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }
}
