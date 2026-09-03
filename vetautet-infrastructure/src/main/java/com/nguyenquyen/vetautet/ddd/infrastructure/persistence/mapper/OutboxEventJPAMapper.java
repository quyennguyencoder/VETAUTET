package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJPAMapper extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 0 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPending(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, e.publishedAt = :publishedAt WHERE e.id = :id")
    int markPublishedById(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, e.publishedAt = :publishedAt WHERE e.id IN :ids")
    int markPublishedByIds(@Param("ids") List<Long> ids, @Param("publishedAt") LocalDateTime publishedAt);
}