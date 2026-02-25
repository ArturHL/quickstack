package com.quickstack.auth.config;

import com.quickstack.auth.security.JwtAuthenticationFilter;
import com.quickstack.auth.security.RateLimitFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration.
 * <p>
 * ASVS L2 compliant:
 * - Stateless sessions (JWT-based)
 * - Argon2id password hashing
 * - CSRF disabled (stateless API)
 * - CORS from CorsConfig
 * - Rate limiting filter before JWT authentication
 * - JWT authentication filter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            CorsConfigurationSource corsConfigurationSource,
            @Autowired(required = false) JwtAuthenticationFilter jwtAuthenticationFilter,
            @Autowired(required = false) RateLimitFilter rateLimitFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - stateless API with JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (actuator)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Public auth endpoints (no JWT required)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password")
                        .permitAll()

                        // Cookie-based auth endpoints (refresh token, no JWT required)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()

                        // Session management requires JWT
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // Catalog requires JWT (not public yet)
                        .requestMatchers("/api/v1/categories/**", "/api/v1/products/**", "/api/v1/menu",
                                "/api/v1/modifier-groups/**", "/api/v1/modifiers/**",
                                "/api/v1/combos/**").authenticated()

                        // Branch management requires JWT
                        .requestMatchers("/api/v1/branches/**", "/api/v1/areas/**",
                                "/api/v1/tables/**").authenticated()

                        // POS customer management requires JWT
                        .requestMatchers("/api/v1/customers/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated())

                // Disable form login and basic auth (API only)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        // Add JWT filter if available (validates Bearer tokens)
        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        // Add Rate Limit filter before JWT filter (checks limits before any processing)
        if (rateLimitFilter != null) {
            Class<? extends Filter> beforeClass = jwtAuthenticationFilter != null
                    ? jwtAuthenticationFilter.getClass()
                    : UsernamePasswordAuthenticationFilter.class;
            http.addFilterBefore(rateLimitFilter, beforeClass);
        }

        return http.build();
    }

    /**
     * Password encoder using Argon2id.
     * ASVS V2.4.1: Use bcrypt, scrypt, argon2, or PBKDF2.
     * <p>
     * Parameters (OWASP recommended for Argon2id):
     * - Salt length: 16 bytes
     * - Hash length: 32 bytes
     * - Parallelism: 1
     * - Memory: 64MB
     * - Iterations: 3
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
