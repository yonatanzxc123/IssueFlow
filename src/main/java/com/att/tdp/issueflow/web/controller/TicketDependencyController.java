package com.att.tdp.issueflow.web.controller;

import com.att.tdp.issueflow.service.TicketDependencyService;
import com.att.tdp.issueflow.web.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.web.dto.response.DependencyResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
public class TicketDependencyController {

    private final TicketDependencyService ticketDependencyService;

    public TicketDependencyController(TicketDependencyService ticketDependencyService) {
        this.ticketDependencyService = ticketDependencyService;
    }

    @PostMapping
    public DependencyResponse addDependency(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddDependencyRequest request
    ) {
        return ticketDependencyService.addDependency(ticketId, request);
    }

    @GetMapping
    public List<DependencyResponse> getDependencies(@PathVariable Long ticketId) {
        return ticketDependencyService.getDependencies(ticketId);
    }

    @DeleteMapping("/{blockerId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable Long ticketId,
            @PathVariable Long blockerId
    ) {
        ticketDependencyService.removeDependency(ticketId, blockerId);
        return ResponseEntity.ok().build();
    }
}
