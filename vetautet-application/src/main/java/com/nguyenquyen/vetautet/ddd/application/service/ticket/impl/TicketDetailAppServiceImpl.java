package com.nguyenquyen.vetautet.ddd.application.service.ticket.impl;


import com.nguyenquyen.vetautet.ddd.application.model.TicketDetailDTO;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.TicketDetailAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TicketDetailAppServiceImpl implements TicketDetailAppService {

    @Override
    public TicketDetailDTO getTicketDetailById(Long ticketId, Long version) {
        return null;
    }

    @Override
    public boolean orderTicketByUser(Long ticketId) {
        return false;
    }
}
