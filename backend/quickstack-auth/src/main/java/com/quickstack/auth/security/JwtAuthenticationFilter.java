package com.quickstack.auth.security;

import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security filter for JWT authentication.
 * <p>
 * Extracts JWT from the Authorization header, validates it,
 * and sets the authentication in the SecurityContext.
 * <p>
 * ASVS Compliance:
 * - V3.5.2: Validates token on every request
 * - V3.5.3: Integrates with Spring Security context
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            // No token present - continue without authentication
            // Public endpoints will work, protected endpoints will get 401 from Spring Security
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.validateToken(token);
            setAuthentication(claims, request);
            log.debug("Authenticated user {} for request {}", claims.getSubject(), request.getRequestURI());

        } catch (InvalidTokenException e) {
            log.debug("Token validation failed for {}: {}", request.getRequestURI(), e.getReason());
            // Clear any existing authentication
            SecurityContextHolder.clearContext();
            // Don't set error response here - let Spring Security handle 401
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header.
     *
     * @param request HTTP request
     * @return JWT token string or null if not present/invalid format
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Authorization header does not start with Bearer prefix");
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (!StringUtils.hasText(token)) {
            log.debug("Bearer token is empty");
            return null;
        }

        return token;
    }

    /**
     * Creates and sets the Authentication object in SecurityContext.
     *
     * @param claims validated JWT claims
     * @param request HTTP request for details
     */
    private void setAuthentication(Claims claims, HttpServletRequest request) {
        UUID userId = jwtService.getUserId(claims);
        UUID tenantId = jwtService.getTenantId(claims);
        UUID roleId = jwtService.getRoleId(claims);
        UUID branchId = jwtService.getBranchId(claims);
        String email = jwtService.getEmail(claims);

        // Create principal with user context
        JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(
                userId, tenantId, roleId, branchId, email);

        // Create authentication token
        // Note: Authorities will be loaded from database in future sprint
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null, // No credentials needed - token already validated
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
