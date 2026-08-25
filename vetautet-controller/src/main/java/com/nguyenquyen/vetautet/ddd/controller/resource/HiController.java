package com.nguyenquyen.vetautet.ddd.controller.resource;

import com.nguyenquyen.vetautet.ddd.application.service.event.EventApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HiController {

    @Autowired
    private EventApplicationService eventApplicationService;

    @RequestMapping()
    public String hello(){
        return eventApplicationService.sayHi("Nguyen Quyen");
    }
}
