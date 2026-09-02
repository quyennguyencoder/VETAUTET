package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.OrderQueue;
import com.nguyenquyen.vetautet.ddd.domain.repository.OrderQueueRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.OrderQueueJPAMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class OrderQueueRepositoryImpl implements OrderQueueRepository {

    @Autowired
    private OrderQueueJPAMapper orderQueueJPAMapper;

    @Override
    public OrderQueue save(OrderQueue orderQueue) {
        return orderQueueJPAMapper.save(orderQueue);
    }

    @Override
    public Optional<OrderQueue> findByToken(String token) {
        return orderQueueJPAMapper.findByToken(token);
    }

    @Override
    @Transactional
    public boolean updateStatus(String token, int status, String orderNumber, String message) {
        int rows = orderQueueJPAMapper.updateStatusByToken(token, status, orderNumber, message);
        return rows > 0;
    }
}
