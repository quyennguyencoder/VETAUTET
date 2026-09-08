package com.nguyenquyen.vetautet.ddd.application.service.ticket.impl;


import com.nguyenquyen.vetautet.ddd.application.mapper.TicketDetailMapper;
import com.nguyenquyen.vetautet.ddd.application.model.TicketDetailDTO;
import com.nguyenquyen.vetautet.ddd.application.model.cache.TicketDetailCache;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.TicketDetailAppService;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.cache.TicketDetailCacheServiceRefactor;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDetailAppServiceImpl implements TicketDetailAppService {

    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketDetailCacheService ticketDetailCacheService;
    private final TicketDetailCacheServiceRefactor ticketDetailCacheServiceRefactor;

    @Override
    public TicketDetailDTO getTicketDetailById(Long ticketId, Long version) {
//        log.info("Implement Application : {}, {}: ", ticketId, version);
        TicketDetailCache ticketDetailCache = ticketDetailCacheServiceRefactor.getTicketDetail(ticketId, version);
        // mapper to DTO
        TicketDetailDTO ticketDetailDTO = TicketDetailMapper.toDTO(ticketDetailCache.getTicketDetail());
        ticketDetailDTO.setVersion(ticketDetailCache.getVersion());
        return ticketDetailDTO;
    }

    @Override
    public boolean orderTicketByUser(Long ticketId) {
        return ticketDetailCacheServiceRefactor.orderTicketByUser(ticketId);
    }


}
