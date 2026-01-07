package taf.yugioh.scanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting filter to prevent abuse.
 * Limits requests per IP address.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    // Rate limit configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int AUTH_MAX_REQUESTS_PER_MINUTE = 10; // Stricter for auth endpoints
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute

    // Store request counts per IP
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        String path = request.getRequestURI();

        // Determine rate limit based on endpoint
        int maxRequests = isAuthEndpoint(path) ? AUTH_MAX_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        // Create unique key for IP + endpoint type
        String key = clientIp + (isAuthEndpoint(path) ? "-auth" : "-general");

        RateLimitInfo info = requestCounts.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();

            if (existing == null || now - existing.windowStart > WINDOW_SIZE_MS) {
                // Start new window
                return new RateLimitInfo(now, 1);
            } else {
                // Increment counter in current window
                existing.count.incrementAndGet();
                return existing;
            }
        });

        if (info.count.get() > maxRequests) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please wait a moment.\"}");
            return;
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxRequests - info.count.get())));

        filterChain.doFilter(request, response);
    }

    /**
     * Get client IP, handling proxies (like Heroku)
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Check if path is an authentication endpoint (stricter limits)
     */
    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/forgot-password") ||
                path.startsWith("/api/auth/reset-password");
    }

    /**
     * Periodically clean up old entries (called by scheduled task)
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > WINDOW_SIZE_MS * 2
        );
    }

    /**
     * Inner class to track rate limit info
     */
    private static class RateLimitInfo {
        final long windowStart;
        final AtomicInteger count;

        RateLimitInfo(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}