package com.nguyenquyen.vetautet.ddd.domain.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository {
    void save(OutboxEvent event);
    List<OutboxEvent> findPendingBatch(int limit);
    void markPublished(Long id, LocalDateTime publishedAt);
    void markPublishedBatch(List<Long> ids, LocalDateTime publishedAt);
}