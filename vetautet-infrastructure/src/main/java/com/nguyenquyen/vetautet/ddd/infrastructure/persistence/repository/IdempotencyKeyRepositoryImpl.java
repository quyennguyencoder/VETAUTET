package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.repository.IdempotencyKeyRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.IdempotencyKeyJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepository {

    private final IdempotencyKeyJPAMapper idempotencyKeyJPAMapper;

    @Override
    public boolean tryInsert(String token, LocalDateTime expiresAt) {
        int affected = idempotencyKeyJPAMapper.insertIgnore(token, LocalDateTime.now(), expiresAt);
        return affected == 1;
    }
}