package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IdempotencyKeyJPAMapper extends JpaRepository<IdempotencyKey, String> {

    // INSERT IGNORE: trả 1 nếu token mới, trả 0 nếu duplicate (silent — không throw exception)
    @Modifying
    @Query(
        value = "INSERT IGNORE INTO idempotency_key (token, created_at, expires_at) VALUES (:token, :createdAt, :expiresAt)",
        nativeQuery = true
    )
    int insertIgnore(
        @Param("token") String token,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("expiresAt") LocalDateTime expiresAt
    );
}