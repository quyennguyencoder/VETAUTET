package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.TicketDetail;
import com.nguyenquyen.vetautet.ddd.domain.repository.TicketStockRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.TicketStockJPAMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TicketStockRepositoryImpl implements TicketStockRepository {

    @Autowired
    private TicketStockJPAMapper ticketStockJPAMapper;

    @Override
    public boolean decreaseStockByAtomicUpdate(Long tickerId, int quantity) {
        log.info("Run test:decreaseStockLevel1 with: | {}, {} ", tickerId, quantity);
        return ticketStockJPAMapper.decreaseStockByAtomicUpdate(tickerId, quantity) > 0;
    }

    @Override
    @Transactional
    public boolean decreaseStockByPessimisticLock(Long tickerId, int quantity) {
        log.info("Run test:decreaseStockLevel2 (PESSIMISTIC LOCK) with: | {}, {} ", tickerId, quantity);

        // 1. Lấy dữ liệu lên và KHÓA LẠI (Luồng khác đến sau sẽ bị treo ở dòng này)
        TicketDetail ticket = ticketStockJPAMapper.getTicketDetailForUpdate(tickerId);

        // 2. Kiểm tra nghiệp vụ bằng Code Java
        if (ticket != null && ticket.getStockAvailable() >= quantity) {
            // 3. Trừ vé
            ticket.setStockAvailable(ticket.getStockAvailable() - quantity);

            // 4. Lưu xuống Database
            ticketStockJPAMapper.save(ticket);

            // Hàm kết thúc -> Transaction kết thúc -> Database mở khóa (Release Lock)
            return true;
        }

        return false;
    }

    @Override
    public boolean decreaseStockByOptimisticLockCAS(Long tickerId, int oldStockAvailable, int quantity) {
        log.info("Run test:decreaseStockLevel3CAS with: | {}, {}, {} ", tickerId, oldStockAvailable, quantity);
        return ticketStockJPAMapper.decreaseStockByOptimisticLockCAS(tickerId, oldStockAvailable, quantity) > 0;
    }

    @Override
    public boolean increaseStock(Long tickerId, int quantity) {
        log.info("Rollback stock: increaseStock for ticketId: {} | quantity: {}", tickerId, quantity);
        // Gọi method increaseStock mà bạn đã thêm vào TicketOrderJPAMapper (Bước 2.1 ở tin nhắn trước)
        return ticketStockJPAMapper.increaseStock(tickerId, quantity) > 0;
    }
}
