package com.nguyenquyen.vetautet.ddd.application.service.ticket;


import com.nguyenquyen.vetautet.ddd.application.model.TicketDTO;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketDetailCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.UpdateTicketCommand;

import java.util.List;

public interface TicketAppService {

    /**
     * Tạo ticket mới
     * @param createRequest Request từ Controller
     * @param createDetailRequest TicketDetail request
     * @return TicketDTO
     */
    TicketDTO createTicket(CreateTicketCommand createRequest, CreateTicketDetailCommand createDetailRequest);

    /**
     * Lấy thông tin ticket
     * @param ticketId
     * @return TicketDTO
     */
    TicketDTO getTicketById(Long ticketId);

    /**
     * Cập nhật ticket
     * @param ticketId
     * @param updateRequest Request từ Controller
     * @return TicketDTO
     */
    TicketDTO updateTicket(Long ticketId, UpdateTicketCommand updateRequest);

    /**
     * Kích hoạt ticket
     * @param ticketId
     * @return TicketDTO
     */
    TicketDTO activeTicket(Long ticketId);

    /**
     * Vô hiệu hóa ticket
     * @param ticketId
     * @return TicketDTO
     */
    TicketDTO inactiveTicket(Long ticketId);

    /**
     * Xoá ticket (soft delete)
     * @param ticketId
     */
    void deleteTicket(Long ticketId);

    List<TicketDTO> getAllActiveTickets();
}
