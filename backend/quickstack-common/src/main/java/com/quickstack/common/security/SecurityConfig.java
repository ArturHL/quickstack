package com.quickstack.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID tenantId = jwt.hasClaim("tid") ? UUID.fromString(jwt.getClaimAsString("tid")) : null;
            UUID roleId = jwt.hasClaim("rid") ? UUID.fromString(jwt.getClaimAsString("rid")) : null;
            UUID branchId = jwt.hasClaim("bid") ? UUID.fromString(jwt.getClaimAsString("bid")) : null;
            String email = jwt.hasClaim("email") ? jwt.getClaimAsString("email") : null;

            JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(userId, tenantId, roleId, branchId, email);
            
            return new UsernamePasswordAuthenticationToken(principal, jwt, Collections.emptyList());
        };
    }
}
