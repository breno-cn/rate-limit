package com.example.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

import static java.util.Objects.isNull;

@Component
public class RateLimitFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final Integer MAX_REQUESTS_PER_MINUTE = 10;

    private static final Long REFILL_RATE = 10L;

    private static final Duration REDIS_DEFAULT_DURATION = Duration.ofMinutes(1L);


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    private Boolean canMakeRequest(String key) {
        String countData = redisTemplate.opsForValue()
                .get(key);

        if (isNull(countData)) {
            redisTemplate.opsForValue()
                    .set(key, "1", REDIS_DEFAULT_DURATION);
            return true;
        }

        redisTemplate.opsForValue()
                .increment(key);

        Long count = Long.parseLong(countData);
        return count <= MAX_REQUESTS_PER_MINUTE;
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String msisdn = httpRequest.getHeader("x-msisdn");
        String key = "msisdn:" + msisdn;
        log.info("key = {}", key);

        if (!canMakeRequest(key)) {
            HttpServletResponse httpResponse =  (HttpServletResponse) servletResponse;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
