package com.forge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        // Key on the RIGHTMOST hop of X-Forwarded-For — the entry appended by the trusted
        // edge proxy (Render), which a client cannot control. Leftmost entries are attacker-
        // supplied; with forward-headers-strategy=framework the rewritten remoteAddr would
        // reflect that spoofable value, so XFF is resolved here instead.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] hops = xff.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (isIpLiteral(hop)) {
                    return truncate(hop);
                }
            }
            // XFF present but unparseable -> collapse to a single shared bucket so clients
            // cannot churn unlimited buckets with malformed headers.
            return "unknown";
        }
        String ip = request.getRemoteAddr();
        if (ip == null) return "unknown";
        return truncate(ip.trim());
    }

    private static boolean isIpLiteral(String value) {
        if (value == null || value.isEmpty()) return false;
        return value.matches("\\d{1,3}(\\.\\d{1,3}){3}") || value.contains(":");
    }

    private static String truncate(String ip) {
        if (ip.length() > 64) return ip.substring(0, 64);
        return ip.isEmpty() ? "unknown" : ip;
    }

    private void evictIfNeeded() {
        if (buckets.size() < MAX_BUCKETS) return;
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess() > IDLE_EVICT_MS);
    }

    private static class Bucket {
        private static final double TOKENS_PER_MS = (double) MAX_REQUESTS / WINDOW_MS;

        private double tokens = MAX_REQUESTS;
        private long lastRefill = System.currentTimeMillis();
        private volatile long lastAccess = System.currentTimeMillis();

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            tokens = Math.min(MAX_REQUESTS, tokens + (now - lastRefill) * TOKENS_PER_MS);
            lastRefill = now;
            lastAccess = now;
            if (tokens < 1.0) return false;
            tokens -= 1.0;
            return true;
        }

        long lastAccess() {
            return lastAccess;
        }
    }
}
