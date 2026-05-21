package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.TicketService;
import com.att.tdp.issueflow.service.TicketCsvService;
import com.att.tdp.issueflow.web.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.web.dto.response.ImportTicketsResponse;
import com.att.tdp.issueflow.web.dto.response.TicketResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCsvService ticketCsvService;

    public TicketController(TicketService ticketService, TicketCsvService ticketCsvService) {
        this.ticketService = ticketService;
        this.ticketCsvService = ticketCsvService;
    }

    @GetMapping
    public List<TicketResponse> getTicketsByProject(@RequestParam Long projectId) {
        return ticketService.getTicketsByProject(projectId);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportTickets(@RequestParam Long projectId) {
        String csv = ticketCsvService.exportTickets(projectId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("tickets-project-" + projectId + ".csv")
                        .build()
                        .toString())
                .body(csv);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportTicketsResponse importTickets(
            @RequestParam Long projectId,
            @RequestParam MultipartFile file
    ) {
        return ticketCsvService.importTickets(projectId, file);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@PathVariable Long ticketId) {
        return ticketService.getTicket(ticketId);
    }

    @PostMapping
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<Void> updateTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        ticketService.updateTicket(ticketId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketResponse> getDeletedTickets(@RequestParam Long projectId) {
        return ticketService.getDeletedTickets(projectId);
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse restoreTicket(@PathVariable Long ticketId) {
        return ticketService.restoreTicket(ticketId);
    }
}
