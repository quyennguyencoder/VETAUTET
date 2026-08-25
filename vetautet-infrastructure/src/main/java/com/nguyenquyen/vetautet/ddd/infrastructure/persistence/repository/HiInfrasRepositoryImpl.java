package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;

import com.nguyenquyen.vetautet.ddd.domain.repository.HiDomainRepository;
import org.springframework.stereotype.Repository;

@Repository
public class HiInfrasRepositoryImpl implements HiDomainRepository {
    @Override
    public String sayHi(String name) {
        return "hi infrastructure repository " + name;
    }
}
