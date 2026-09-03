package com.nguyenquyen.vetautet.ddd.controller.http;

import com.nguyenquyen.vetautet.ddd.application.service.event.EventAppService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private EventAppService eventAppService;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/hello/v1")
    @RateLimiter(name="backendA", fallbackMethod = "fallbackHello")
    public String hello(){
        return eventAppService.sayHi("Nguyen Quyen");
    }

    @RequestMapping("/hello/v2")
    @RateLimiter(name="backendB", fallbackMethod = "fallbackHello")
    public String hello2(){
        return eventAppService.sayHi("Nguyen Quyen 2");
    }

    public String fallbackHello(Throwable throwable){
        return "Too many requests, please try again later.";
    }

    private static final SecureRandom random = new SecureRandom();

    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = "checkRandom", fallbackMethod = "fallbackCircuitBreaker")
    public String circuitBreaker(){
        int productId = random.nextInt(20) +1;
        String url = "https://fakestoreapi.com/products/"+productId;
        return restTemplate.getForObject(url, String.class);
    }
    public String fallbackCircuitBreaker(Throwable throwable){
//        return throwable.getMessage();
        return "service error";
    }
}
