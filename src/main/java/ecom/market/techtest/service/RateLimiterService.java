package ecom.market.techtest.service;

import ecom.market.techtest.exception.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class RateLimiterService {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    @Value("${rateLimiter.maxRequestsPerInterval}")
    private int maxRequestsPerInterval;
    @Value("${rateLimiter.intervalInSeconds}")
    private long intervalInSeconds;

    @Around("@annotation(ecom.market.techtest.util.RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ipAddress = request.getHeader(X_FORWARDED_FOR);

        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        AtomicInteger requestCount = requestCounts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        int count = requestCount.get() + 1;
        if (count > maxRequestsPerInterval) {
            throw new RateLimitExceededException("Rate limit exceeded");
        } else {
            requestCount.incrementAndGet();
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            requestCount.decrementAndGet();
        }, intervalInSeconds, TimeUnit.SECONDS);
        System.out.println(requestCounts + " " + ipAddress);
        return joinPoint.proceed();
    }

    public Map<String, AtomicInteger> getRequestCounts() {
        return requestCounts;
    }

    public void setMaxRequestsPerInterval(int maxRequestsPerInterval) {
        this.maxRequestsPerInterval = maxRequestsPerInterval;
    }

    public void setIntervalInSeconds(long intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }
}
