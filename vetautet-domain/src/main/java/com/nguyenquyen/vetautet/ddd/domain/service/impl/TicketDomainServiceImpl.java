package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.Ticket;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.TicketDetail;
import com.nguyenquyen.vetautet.ddd.domain.repository.TicketDetailRepository;
import com.nguyenquyen.vetautet.ddd.domain.repository.TicketRepository;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDomainServiceImpl implements TicketDomainService {

    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;

    @Override
    public Ticket createTicket(Ticket ticket, TicketDetail ticketDetail) {
        // 1. Validate Ticket information
        this.validateTicketCreation(ticket);

        // 2. Set audit fields for Ticket
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setStatus(1); // 1 = ACTIVE

        // 3. Persist Ticket
        Ticket savedTicket = ticketRepository.save(ticket);

        // 4. Create default TicketDetail
        ticketDetail.setActivityId(savedTicket.getId());
        ticketDetail.setCreatedAt(LocalDateTime.now());
        ticketDetail.setUpdatedAt(LocalDateTime.now());
        ticketDetail.setStatus(1); // 1 = ACTIVE

        // Set defaults for nullable fields that DB requires NOT NULL
        if (ticketDetail.getPriceFlash() == null) {
            ticketDetail.setPriceFlash(ticketDetail.getPriceOriginal() != null
                    ? ticketDetail.getPriceOriginal().multiply(BigDecimal.valueOf(0.7))
                    : BigDecimal.ZERO);
        }
        if (ticketDetail.getSaleStartTime() == null) {
            ticketDetail.setSaleStartTime(LocalDateTime.now());
        }
        if (ticketDetail.getSaleEndTime() == null) {
            ticketDetail.setSaleEndTime(ticketDetail.getSaleStartTime().plusDays(30));
        }

        // Validate TicketDetail
        this.validateTicketDetailCreation(ticketDetail);

        // 5. Persist TicketDetail
        ticketDetailRepository.save(ticketDetail);

        log.info("Created ticket with ID: {} and default TicketDetail", savedTicket.getId());
        return savedTicket;
    }

    @Override
    public Ticket getTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
    }

    @Override
    public Ticket updateTicket(Long ticketId, Ticket ticket) {
        return null;
    }

    @Override
    public Ticket activeTicket(Long ticketId) {
        return null;
    }

    @Override
    public Ticket inactiveTicket(Long ticketId) {
        return null;
    }

    @Override
    public void deleteTicket(Long ticketId) {

    }

    @Override
    public List<Ticket> getAllActiveTickets() {
        log.info("Domain Service: Getting all active tickets");
        return ticketRepository.findAllActive();
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate business rules for ticket creation
     */
    private void validateTicketCreation(Ticket ticket) {
        // Rule 1: Name không được trống
        if (ticket.getName() == null || ticket.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket name cannot be empty");
        }

        if (ticket.getName().length() > 255) {
            throw new IllegalArgumentException("Ticket name cannot exceed 255 characters");
        }

        // Rule 2: Description độ dài hợp lệ
        if (ticket.getDescription() != null && ticket.getDescription().length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }

        // Rule 3: startTime phải trước endTime
        if (ticket.getStartTime() != null && ticket.getEndTime() != null) {
            if (ticket.getStartTime().isAfter(ticket.getEndTime())) {
                throw new IllegalArgumentException("Start time must be before end time");
            }
        }

        // Rule 4: startTime không được trong quá khứ
        if (ticket.getStartTime() != null && ticket.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        }
    }

    /**
     * Validate business rules for ticket update
     */
    private void validateTicketUpdate(Ticket existingTicket, Ticket updatedTicket) {
        this.validateTicketCreation(updatedTicket);

        // Additional rule: Cannot update if ticket is DELETED (status = 2)
        if (existingTicket.getStatus() == 2) {
            throw new IllegalArgumentException("Cannot update deleted ticket");
        }
    }

    /**
     * Validate business rules for TicketDetail creation
     */
    private void validateTicketDetailCreation(TicketDetail ticketDetail) {
        // Rule 1: Name không được trống
        if (ticketDetail.getName() == null || ticketDetail.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("TicketDetail name cannot be empty");
        }

        // Rule 2: Stock initial phải > 0
        if (ticketDetail.getStockInitial() <= 0) {
            throw new IllegalArgumentException("Stock initial must be greater than 0");
        }

        // Rule 3: Stock available không vượt quá stock initial
        if (ticketDetail.getStockAvailable() > ticketDetail.getStockInitial()) {
            throw new IllegalArgumentException("Stock available cannot exceed stock initial");
        }

        // Rule 4: Price original phải > 0
        if (ticketDetail.getPriceOriginal() == null
                || ticketDetail.getPriceOriginal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price original must be greater than 0");
        }

        // Rule 5: Sale time validation
        if (ticketDetail.getSaleStartTime() != null && ticketDetail.getSaleEndTime() != null) {
            if (ticketDetail.getSaleStartTime().isAfter(ticketDetail.getSaleEndTime())) {
                throw new IllegalArgumentException("Sale start time must be before sale end time");
            }
        }
    }
}
