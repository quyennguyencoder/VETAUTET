package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.dataobject.PaymentTransactionDO;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJPAMapper extends JpaRepository<PaymentTransactionDO, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransactionDO p SET p.paymentStatus = :status, " +
            "p.gatewayTransactionId = :gwId, p.paymentUrl = :url, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.paymentId = :paymentId")
    int updateStatus(@Param("paymentId") String paymentId,
                     @Param("status") Integer status,
                     @Param("gwId") String gatewayId,
                     @Param("url") String url);
    PaymentTransactionDO findByPaymentId(String paymentId);
}
