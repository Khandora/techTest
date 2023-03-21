package ecom.market.techtest;

import ecom.market.techtest.exception.RateLimitExceededException;
import ecom.market.techtest.service.RateLimiterService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
public class RateLimitedAspectTest {

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        rateLimiterService.setIntervalInSeconds(1);
        rateLimiterService.setMaxRequestsPerInterval(5);
        rateLimiterService.getRequestCounts().clear();
    }

    @Test
    void givenMaxRequestsExceeded_whenRateLimit_thenThrowsRateLimitExceededException() throws Throwable {
        String ipAddress = "192.168.0.1";
        int numRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numRequests);
        CountDownLatch latch = new CountDownLatch(numRequests);

        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                mockRequest.addHeader("X-Forwarded-For", ipAddress);
                ServletRequestAttributes mockRequestAttributes = new ServletRequestAttributes(mockRequest);
                RequestContextHolder.setRequestAttributes(mockRequestAttributes);
                try {
                    rateLimiterService.rateLimit(proceedingJoinPoint);
                } catch (Throwable e) {
                    System.out.println("Exception occurred: " + e.getMessage());
                    assertEquals(RateLimitExceededException.class, e.getCause().getClass());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        assertEquals(5, rateLimiterService.getRequestCounts().get(ipAddress).get());
    }

    @Test
    void givenMaxRequestsNotExceeded_whenRateLimit_thenProceedsSuccessfully() throws Throwable {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int numRequests = 10;
        String[] ipAddresses = {"192.168.0.1", "192.168.0.2", "192.168.0.3", "192.168.0.4", "192.168.0.5"};
        CountDownLatch latch = new CountDownLatch(numRequests);

        for (int i = 0; i < numRequests; i++) {
            String ipAddress = ipAddresses[i % ipAddresses.length];
            executorService.submit(() -> {
                try {
                    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                    mockRequest.addHeader("X-Forwarded-For", ipAddress);
                    ServletRequestAttributes mockRequestAttributes = new ServletRequestAttributes(mockRequest);
                    RequestContextHolder.setRequestAttributes(mockRequestAttributes);
                    rateLimiterService.rateLimit(proceedingJoinPoint);
                } catch (Throwable e) {
                    System.out.println("Exception occurred: " + e.getMessage());
                    assertEquals(RateLimitExceededException.class, e.getCause().getClass());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        for (String ipAddress : ipAddresses) {
            assertEquals(2, rateLimiterService.getRequestCounts().get(ipAddress).get());
        }
    }
}