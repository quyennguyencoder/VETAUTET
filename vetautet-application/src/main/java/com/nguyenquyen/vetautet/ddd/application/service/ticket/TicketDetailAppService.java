package com.nguyenquyen.vetautet.ddd.application.service.ticket;


import com.nguyenquyen.vetautet.ddd.application.model.TicketDetailDTO;

public interface TicketDetailAppService {

    TicketDetailDTO getTicketDetailById(Long ticketId, Long version); // should convert to TickDetailDTO by Application Module
    // order ticket
    boolean orderTicketByUser(Long ticketId);
}
