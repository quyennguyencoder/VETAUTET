package com.nguyenquyen.vetautet.ddd.application.service.event.impl;

import com.nguyenquyen.vetautet.ddd.application.service.event.EventApplicationService;
import com.nguyenquyen.vetautet.ddd.domain.service.HiDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventApplicationServiceImpl implements EventApplicationService {

    @Autowired
    private HiDomainService hiDomainService;

    @Override
    public String sayHi(String name) {
        return hiDomainService.sayHi(name);
    }
}
