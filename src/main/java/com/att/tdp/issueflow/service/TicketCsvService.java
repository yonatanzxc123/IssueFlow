package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.domain.Ticket;
import com.att.tdp.issueflow.domain.User;
import com.att.tdp.issueflow.domain.enums.TicketPriority;
import com.att.tdp.issueflow.domain.enums.TicketStatus;
import com.att.tdp.issueflow.domain.enums.TicketType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.web.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.web.dto.response.ImportTicketsResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketCsvService {

    private static final String[] HEADERS = {
            "id",
            "title",
            "description",
            "status",
            "priority",
            "type",
            "assigneeId"
    };

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final TicketService ticketService;
    private final AuditLogService auditLogService;

    public TicketCsvService(
            TicketRepository ticketRepository,
            ProjectRepository projectRepository,
            TicketService ticketService,
            AuditLogService auditLogService
    ) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.ticketService = ticketService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public String exportTickets(Long projectId) {
        ensureActiveProject(projectId);

        try (StringWriter writer = new StringWriter();
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader(HEADERS)
                        .build())) {
            for (Ticket ticket : ticketRepository.findByProjectIdAndDeletedFalseOrderByIdAsc(projectId)) {
                User assignee = ticket.getAssignee();
                printer.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        nullToEmpty(ticket.getDescription()),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        assignee == null ? "" : assignee.getId()
                );
            }
            printer.flush();
            return writer.toString();
        } catch (IOException exception) {
            throw new BadRequestException("Could not write ticket CSV");
        }
    }

    public ImportTicketsResponse importTickets(Long projectId, MultipartFile file) {
        ensureActiveProject(projectId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required");
        }

        List<String> errors = new ArrayList<>();
        int created = 0;
        int failed = 0;

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .build()
                        .parse(reader)) {
            if (!parser.getHeaderNames().equals(List.of(HEADERS))) {
                failed++;
                errors.add("Row 1: CSV header must be id,title,description,status,priority,type,assigneeId");
            } else {
                for (CSVRecord record : parser) {
                    ImportRowResult result = importRecord(projectId, record);
                    if (result.created()) {
                        created++;
                    } else {
                        failed++;
                        errors.add(result.error());
                    }
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            failed++;
            errors.add("CSV could not be parsed: " + exception.getMessage());
        }

        auditLogService.recordImportAction(projectId);
        return new ImportTicketsResponse(created, failed, errors);
    }

    private ImportRowResult importRecord(Long projectId, CSVRecord record) {
        String row = "Row " + (record.getRecordNumber() + 1);
        if (!record.isConsistent()) {
            return ImportRowResult.failed(row + ": malformed row, expected 7 columns but found " + record.size());
        }

        try {
            String title = requiredText(record, "title", row);
            String description = optionalText(record, "description");
            validateDescription(description);
            TicketStatus status = requiredEnum(record, "status", TicketStatus.class, row);
            TicketPriority priority = requiredEnum(record, "priority", TicketPriority.class, row);
            TicketType type = requiredEnum(record, "type", TicketType.class, row);
            Long assigneeId = optionalLong(record, "assigneeId", row);

            ticketService.createTicket(new CreateTicketRequest(
                    title,
                    description,
                    status,
                    priority,
                    type,
                    projectId,
                    assigneeId,
                    null
            ));
            return ImportRowResult.success();
        } catch (IllegalArgumentException | ResourceNotFoundException exception) {
            return ImportRowResult.failed(row + ": " + exception.getMessage());
        }
    }

    private void ensureActiveProject(Long projectId) {
        projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private String requiredText(CSVRecord record, String field, String row) {
        String value = optionalText(record, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if ("title".equals(field) && value.length() > 255) {
            throw new IllegalArgumentException("title must be at most 255 characters");
        }
        return value;
    }

    private String optionalText(CSVRecord record, String field) {
        String value = record.get(field);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private <T extends Enum<T>> T requiredEnum(CSVRecord record, String field, Class<T> enumType, String row) {
        String value = optionalText(record, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " has invalid value '" + value + "'");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 10000) {
            throw new IllegalArgumentException("description must be at most 10000 characters");
        }
    }

    private Long optionalLong(CSVRecord record, String field, String row) {
        String value = optionalText(record, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " must be a number");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ImportRowResult(boolean created, String error) {

        static ImportRowResult success() {
            return new ImportRowResult(true, null);
        }

        static ImportRowResult failed(String error) {
            return new ImportRowResult(false, error);
        }
    }
}
