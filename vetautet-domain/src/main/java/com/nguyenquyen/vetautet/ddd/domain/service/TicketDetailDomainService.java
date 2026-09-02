package com.nguyenquyen.vetautet.ddd.domain.service;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.TicketDetail;

public interface TicketDetailDomainService {

    TicketDetail getTicketDetailById(Long ticketId);
}
