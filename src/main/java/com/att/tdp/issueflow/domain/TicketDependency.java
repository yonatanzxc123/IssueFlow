package com.att.tdp.issueflow.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ticket_dependencies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_dependencies_ticket_blocker",
                columnNames = {"ticket_id", "blocker_ticket_id"}
        )
)
public class TicketDependency extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_ticket_id", nullable = false)
    private Ticket blockerTicket;

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Ticket getBlockerTicket() {
        return blockerTicket;
    }

    public void setBlockerTicket(Ticket blockerTicket) {
        this.blockerTicket = blockerTicket;
    }
}
