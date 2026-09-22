package com.nguyenquyen.vetautet.ddd.domain.repository;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    User save(User user);
    Optional<User> findById(Long id);
}
