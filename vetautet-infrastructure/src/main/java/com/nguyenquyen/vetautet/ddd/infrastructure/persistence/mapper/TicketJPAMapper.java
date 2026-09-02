package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketJPAMapper extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatus(Integer status);
}
