package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Attachment;
import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.enums.AuditAction;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.web.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.web.mapper.AttachmentMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            TicketRepository ticketRepository,
            AuditLogService auditLogService
    ) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long ticketId) {
        findActiveTicket(ticketId);
        return attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(AttachmentMapper::toResponse)
                .toList();
    }

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        Ticket ticket = findActiveTicket(ticketId);
        validateFile(file);

        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setFilename(safeFilename(file.getOriginalFilename()));
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setContent(readBytes(file));

        Attachment saved = attachmentRepository.save(attachment);
        auditLogService.recordAttachmentAction(AuditAction.UPLOAD_ATTACHMENT, saved.getId());
        return AttachmentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        findActiveTicket(ticketId);
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        attachmentRepository.delete(attachment);
        auditLogService.recordAttachmentAction(AuditAction.DELETE_ATTACHMENT, attachment.getId());
    }

    private Ticket findActiveTicket(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new BadRequestException("Attachment file is required");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("Attachment file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Attachment file size must be 10 MB or less");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException(
                    "Unsupported attachment content type. Allowed types: image/png, image/jpeg, application/pdf, text/plain"
            );
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Could not read attachment file");
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String filename = Paths.get(originalFilename).getFileName().toString();
        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (filename.isBlank() || ".".equals(filename) || "..".equals(filename)) {
            return "attachment";
        }
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }
}
