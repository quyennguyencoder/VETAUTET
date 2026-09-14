package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.TicketDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TicketStockJPAMapper extends JpaRepository<TicketDetail, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
            "t.stockAvailable = t.stockAvailable - :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity")
    int decreaseStockByAtomicUpdate(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketDetail t WHERE t.id = :ticketId")
    TicketDetail getTicketDetailForUpdate(@Param("ticketId") Long ticketId);

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
            "t.stockAvailable = :oldStockAvailable - :quantity " +
            "WHERE t.id = :ticketId AND t.stockAvailable = :oldStockAvailable")
    int decreaseStockByOptimisticLockCAS(@Param("ticketId") Long ticketId, @Param("oldStockAvailable") int oldStockAvailable, @Param("quantity") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
            "t.stockAvailable = t.stockAvailable + :quantity " +
            "WHERE t.id = :ticketId")
    int increaseStock(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);
}
