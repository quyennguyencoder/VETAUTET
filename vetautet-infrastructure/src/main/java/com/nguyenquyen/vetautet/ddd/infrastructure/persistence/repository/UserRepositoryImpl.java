package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;

import com.nguyenquyen.vetautet.ddd.domain.model.entity.User;
import com.nguyenquyen.vetautet.ddd.domain.repository.UserRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.UserJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJPAMapper userJPAMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJPAMapper.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userJPAMapper.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJPAMapper.findById(id);
    }
}
