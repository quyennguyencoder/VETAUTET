package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.OrderQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderQueueJPAMapper extends JpaRepository<OrderQueue, Long> {

    Optional<OrderQueue> findByToken(String token);

    @Modifying
    @Query("UPDATE OrderQueue q SET q.status = :status, q.orderNumber = :orderNumber, q.message = :message WHERE q.token = :token")
    int updateStatusByToken(@Param("token") String token,
                            @Param("status") int status,
                            @Param("orderNumber") String orderNumber,
                            @Param("message") String message);
}