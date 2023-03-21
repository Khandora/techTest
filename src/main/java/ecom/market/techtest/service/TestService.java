package ecom.market.techtest.service;

import ecom.market.techtest.util.RateLimited;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @RateLimited
    public String hello() {
        return "Method from service is called!";
    }
}
