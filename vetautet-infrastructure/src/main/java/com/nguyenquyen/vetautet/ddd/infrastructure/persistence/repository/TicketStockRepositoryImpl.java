package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.repository.TicketStockRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.TicketStockJPAMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TicketStockRepositoryImpl implements TicketStockRepository {

    @Autowired
    private TicketStockJPAMapper ticketStockJPAMapper;

    @Override
    public boolean decreaseStockLevel1(Long tickerId, int quantity) {
        log.info("Run test:decreaseStockLevel1 with: | {}, {} ", tickerId, quantity);
        return ticketStockJPAMapper.decreaseStockLevel1(tickerId, quantity) > 0;
    }

    @Override
    public boolean decreaseStockLevel2(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel3CAS(Long tickerId, int oldStockAvailable, int quantity) {
        log.info("Run test:decreaseStockLevel3CAS with: | {}, {}, {} ", tickerId, oldStockAvailable, quantity);
        return ticketStockJPAMapper.decreaseStockLevel3CAS(tickerId, oldStockAvailable, quantity) > 0;
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketStockJPAMapper.getStockAvailable(ticketId);
    }

    @Override
    public boolean increaseStock(Long tickerId, int quantity) {
        log.info("Rollback stock: increaseStock for ticketId: {} | quantity: {}", tickerId, quantity);
        // Gọi method increaseStock mà bạn đã thêm vào TicketOrderJPAMapper (Bước 2.1 ở tin nhắn trước)
        return ticketStockJPAMapper.increaseStock(tickerId, quantity) > 0;
    }
}
