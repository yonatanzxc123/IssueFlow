package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.TicketService;
import com.att.tdp.issueflow.web.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.web.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.web.dto.response.TicketResponse;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResponse> getTicketsByProject(@RequestParam Long projectId) {
        return ticketService.getTicketsByProject(projectId);
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
