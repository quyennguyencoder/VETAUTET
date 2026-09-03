package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.Ticket;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.TicketDetail;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class TicketDomainServiceImpl implements TicketDomainService {

    @Override
    public Ticket createTicket(Ticket ticket, TicketDetail ticketDetail) {
        return null;
    }

    @Override
    public Ticket getTicketById(Long ticketId) {
        return null;
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
        return List.of();
    }
}
