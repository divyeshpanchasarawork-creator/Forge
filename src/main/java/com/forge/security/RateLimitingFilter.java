package com.forge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;
    private static final long IDLE_EVICT_MS = 10 * 60_000L;
    private static final int MAX_BUCKETS = 10_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        evictIfNeeded();

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket());
        if (!bucket.tryConsume()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // With server.forward-headers-strategy=framework the ForwardedHeaderFilter runs
        // first (order Integer.MIN_VALUE) and rewrites remoteAddr to the client IP from
        // X-Forwarded-For, so this never trusts a raw client-supplied header directly.
        String ip = request.getRemoteAddr();
        if (ip == null) return "unknown";
        ip = ip.trim();
        if (ip.length() > 64) ip = ip.substring(0, 64);
        return ip.isEmpty() ? "unknown" : ip;
    }

    private void evictIfNeeded() {
        if (buckets.size() < MAX_BUCKETS) return;
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess() > IDLE_EVICT_MS);
    }

    private static class Bucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private volatile long lastAccess = System.currentTimeMillis();

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            long ws = windowStart;
            if (now - ws > WINDOW_MS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            lastAccess = now;
            return count.incrementAndGet() <= MAX_REQUESTS;
        }

        long lastAccess() {
            return lastAccess;
        }
    }
}
