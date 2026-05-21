package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.AttachmentService;
import com.att.tdp.issueflow.web.dto.response.AttachmentResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public List<AttachmentResponse> getAttachments(@PathVariable Long ticketId) {
        return attachmentService.getAttachments(ticketId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam(required = false) MultipartFile file
    ) {
        return attachmentService.uploadAttachment(ticketId, file);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.deleteAttachment(ticketId, attachmentId);
        return ResponseEntity.ok().build();
    }
}
