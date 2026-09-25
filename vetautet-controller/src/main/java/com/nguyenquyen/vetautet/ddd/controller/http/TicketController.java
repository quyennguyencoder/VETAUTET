package com.nguyenquyen.vetautet.ddd.controller.http;


import com.nguyenquyen.vetautet.ddd.application.model.TicketDTO;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketDetailCommand;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.TicketAppService;
import com.nguyenquyen.vetautet.ddd.controller.dto.CreateTicketFullRequest;
import com.nguyenquyen.vetautet.ddd.controller.dto.UpdateTicketRequest;
import com.nguyenquyen.vetautet.ddd.controller.mapper.TicketControllerMapper;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ticket")
@Slf4j
@RequiredArgsConstructor
public class TicketController {


    private final TicketAppService ticketAppService;


    @GetMapping("/active")
    public ResultMessage<List<TicketDTO>> getAllActiveTickets() {
        log.info("Fetching all active tickets");
        List<TicketDTO> tickets = ticketAppService.getAllActiveTickets();
        return ResultUtil.data(tickets);
    }

    /**
     * Tạo ticket mới
     *
     * POST /ticket/create
     *
     * Request Body:
     {
     "ticket": {
     "name": "Concert ABC",
     "description": "Concert held in HCM City",
     "startTime": "2024-05-01 18:00:00",
     "endTime": "2024-05-01 22:00:00"
     },
     "detail": {
     "name": "VIP",
     "stockInitial": 100,
     "stockAvailable": 100,
     "priceOriginal": 500000
     }
     }
     *
     * @param request
     * @return ResultMessage<TicketDTO>
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResultMessage<TicketDTO> createTicket(
            @Valid @RequestBody CreateTicketFullRequest request) {
        log.info("Creating ticket: {}", request.getTicket().getName());
        CreateTicketCommand ticketCmd = TicketControllerMapper.toCommand(request.getTicket());
        CreateTicketDetailCommand detailCmd = TicketControllerMapper.toDetailCommand(request.getDetail());
        TicketDTO ticketDTO = ticketAppService.createTicket(ticketCmd, detailCmd);
        return ResultUtil.data(ticketDTO);
    }

    @GetMapping("/{ticketId}")
    public ResultMessage<TicketDTO> getTicket(@PathVariable("ticketId") Long ticketId) {
        log.info("Fetching ticket: {}", ticketId);
        TicketDTO ticketDTO = ticketAppService.getTicketById(ticketId);
        return ResultUtil.data(ticketDTO);
    }

    @PutMapping("/{ticketId}")
    public ResultMessage<TicketDTO> updateTicket(
            @PathVariable("ticketId") Long ticketId,
            @Valid @RequestBody UpdateTicketRequest updateRequest) {
        log.info("Updating ticket: {}", ticketId);
        // TicketDTO ticketDTO = ticketAppService.updateTicket(ticketId, updateRequest);
        return ResultUtil.data(null);
    }

    @PutMapping("/{ticketId}/active")
    public ResultMessage<TicketDTO> activeTicket(@PathVariable("ticketId") Long ticketId) {
        log.info("Activating ticket: {}", ticketId);
        TicketDTO ticketDTO = ticketAppService.activeTicket(ticketId);
        return ResultUtil.data(ticketDTO);
    }

    @PutMapping("/{ticketId}/inactive")
    public ResultMessage<TicketDTO> inactiveTicket(@PathVariable("ticketId") Long ticketId) {
        log.info("Inactivating ticket: {}", ticketId);
        TicketDTO ticketDTO = ticketAppService.inactiveTicket(ticketId);
        return ResultUtil.data(ticketDTO);
    }

    @DeleteMapping("/{ticketId}")
    public ResultMessage<String> deleteTicket(@PathVariable("ticketId") Long ticketId) {
        log.info("Deleting ticket: {}", ticketId);
        ticketAppService.deleteTicket(ticketId);
        return ResultUtil.data("Ticket deleted successfully");
    }
}
