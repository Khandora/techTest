package ecom.market.techtest.controller;

import ecom.market.techtest.service.TestService;
import ecom.market.techtest.util.RateLimited;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/test")
    @RateLimited
    public ResponseEntity<HttpStatus> testEndpoint() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/testService")
    public String testEndpointWithServiceLayer() {
        return testService.hello();
    }
}
