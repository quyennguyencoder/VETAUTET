package com.nguyenquyen.vetautet.ddd.application.service.ticket.impl;


import com.nguyenquyen.vetautet.ddd.application.model.TicketDTO;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketDetailCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.UpdateTicketCommand;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.TicketAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TicketAppServiceImpl implements TicketAppService {

    @Override
    public TicketDTO createTicket(CreateTicketCommand createRequest, CreateTicketDetailCommand createDetailRequest) {
        return null;
    }

    @Override
    public TicketDTO getTicketById(Long ticketId) {
        return null;
    }

    @Override
    public TicketDTO updateTicket(Long ticketId, UpdateTicketCommand updateRequest) {
        return null;
    }

    @Override
    public TicketDTO activeTicket(Long ticketId) {
        return null;
    }

    @Override
    public TicketDTO inactiveTicket(Long ticketId) {
        return null;
    }

    @Override
    public void deleteTicket(Long ticketId) {

    }

    @Override
    public List<TicketDTO> getAllActiveTickets() {
        return List.of();
    }
}
