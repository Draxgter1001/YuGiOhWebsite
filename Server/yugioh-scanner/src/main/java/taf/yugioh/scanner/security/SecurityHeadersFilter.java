package taf.yugioh.scanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security headers to all responses for production hardening.
 * These headers help prevent common web vulnerabilities.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Prevent clickjacking - don't allow site to be embedded in iframes
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Enable XSS filter in browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Control referrer information sent with requests
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy - disable unnecessary browser features
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        // Cache control for API responses (don't cache sensitive data)
        if (request.getRequestURI().startsWith("/api/auth/") ||
                request.getRequestURI().startsWith("/api/decks")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }
}