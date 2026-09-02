package com.nguyenquyen.vetautet.ddd.domain.repository;

import java.time.LocalDateTime;

public interface IdempotencyKeyRepository {

    /**
     * INSERT IGNORE — atomic idempotency gate.
     *
     * @return true  → token mới, tiếp tục xử lý
     *         false → duplicate (Kafka retry / rebalance), skip
     */
    boolean tryInsert(String token, LocalDateTime expiresAt);
}