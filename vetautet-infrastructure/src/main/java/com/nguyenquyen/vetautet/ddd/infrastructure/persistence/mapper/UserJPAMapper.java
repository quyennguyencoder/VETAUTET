package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJPAMapper extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
