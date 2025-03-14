package com.example.demo.interceptor;

import com.example.demo.config.RateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();
        String key = "rate_limit:" + clientIp;
        
        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            // First request in window
            redisTemplate.opsForValue().set(key, "1", rateLimitConfig.getRateLimitWindow(), TimeUnit.SECONDS);
            return true;
        }
        
        int count = Integer.parseInt(currentCount);
        if (count >= rateLimitConfig.getRateLimitMaxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
} 