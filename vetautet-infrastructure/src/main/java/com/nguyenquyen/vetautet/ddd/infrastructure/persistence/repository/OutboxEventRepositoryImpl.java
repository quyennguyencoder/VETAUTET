package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.OutboxEvent;
import com.nguyenquyen.vetautet.ddd.domain.repository.OutboxEventRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.OutboxEventJPAMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    @Autowired
    private OutboxEventJPAMapper outboxEventJPAMapper;

    @Override
    public void save(OutboxEvent event) {
        outboxEventJPAMapper.save(event);
    }

    @Override
    public List<OutboxEvent> findPendingBatch(int limit) {
        return outboxEventJPAMapper.findPending(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markPublished(Long id, LocalDateTime publishedAt) {
        outboxEventJPAMapper.markPublishedById(id, publishedAt);
    }

    @Override
    @Transactional
    public void markPublishedBatch(List<Long> ids, LocalDateTime publishedAt) {
        outboxEventJPAMapper.markPublishedByIds(ids, publishedAt);
    }
}