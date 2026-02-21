package com.quickstack.auth.security;

import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.common.exception.InvalidTokenException.TokenType;
import com.quickstack.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for JwtAuthenticationFilter.
 */
@DisplayName("JwtAuthenticationFilter")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    // Test data
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ROLE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BRANCH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private Claims createValidClaims() {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("sub", USER_ID.toString());
        claimsMap.put(JwtService.CLAIM_EMAIL, EMAIL);
        claimsMap.put(JwtService.CLAIM_TENANT_ID, TENANT_ID.toString());
        claimsMap.put(JwtService.CLAIM_ROLE_ID, ROLE_ID.toString());
        claimsMap.put(JwtService.CLAIM_BRANCH_ID, BRANCH_ID.toString());
        return new DefaultClaims(claimsMap);
    }

    @Nested
    @DisplayName("Valid token handling")
    class ValidTokenHandling {

        @Test
        @DisplayName("authenticates request with valid Bearer token")
        void authenticatesRequestWithValidBearerToken() throws ServletException, IOException {
            // Arrange
            String validToken = "valid.jwt.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
            request.setRequestURI("/api/v1/products");

            Claims claims = createValidClaims();
            when(jwtService.validateToken(validToken)).thenReturn(claims);
            when(jwtService.getUserId(claims)).thenReturn(USER_ID);
            when(jwtService.getTenantId(claims)).thenReturn(TENANT_ID);
            when(jwtService.getRoleId(claims)).thenReturn(ROLE_ID);
            when(jwtService.getBranchId(claims)).thenReturn(BRANCH_ID);
            when(jwtService.getEmail(claims)).thenReturn(EMAIL);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();

            JwtAuthenticationPrincipal principal =
                    (JwtAuthenticationPrincipal) auth.getPrincipal();
            assertThat(principal.userId()).isEqualTo(USER_ID);
            assertThat(principal.tenantId()).isEqualTo(TENANT_ID);
            assertThat(principal.roleId()).isEqualTo(ROLE_ID);
            assertThat(principal.branchId()).isEqualTo(BRANCH_ID);
            assertThat(principal.email()).isEqualTo(EMAIL);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("handles null branchId correctly")
        void handlesNullBranchIdCorrectly() throws ServletException, IOException {
            // Arrange
            String validToken = "valid.jwt.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);

            Claims claims = createValidClaims();
            when(jwtService.validateToken(validToken)).thenReturn(claims);
            when(jwtService.getUserId(claims)).thenReturn(USER_ID);
            when(jwtService.getTenantId(claims)).thenReturn(TENANT_ID);
            when(jwtService.getRoleId(claims)).thenReturn(ROLE_ID);
            when(jwtService.getBranchId(claims)).thenReturn(null); // No branch
            when(jwtService.getEmail(claims)).thenReturn(EMAIL);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();

            JwtAuthenticationPrincipal principal =
                    (JwtAuthenticationPrincipal) auth.getPrincipal();
            assertThat(principal.branchId()).isNull();
        }

        @Test
        @DisplayName("sets authentication details from request")
        void setsAuthenticationDetailsFromRequest() throws ServletException, IOException {
            // Arrange
            String validToken = "valid.jwt.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
            request.setRemoteAddr("192.168.1.100");

            Claims claims = createValidClaims();
            when(jwtService.validateToken(validToken)).thenReturn(claims);
            when(jwtService.getUserId(claims)).thenReturn(USER_ID);
            when(jwtService.getTenantId(claims)).thenReturn(TENANT_ID);
            when(jwtService.getRoleId(claims)).thenReturn(ROLE_ID);
            when(jwtService.getBranchId(claims)).thenReturn(BRANCH_ID);
            when(jwtService.getEmail(claims)).thenReturn(EMAIL);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getDetails()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Missing token handling")
    class MissingTokenHandling {

        @Test
        @DisplayName("continues filter chain when no Authorization header")
        void continuesFilterChainWhenNoAuthorizationHeader() throws ServletException, IOException {
            // Arrange - no Authorization header
            request.setRequestURI("/api/v1/auth/login");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("continues filter chain when Authorization header is empty")
        void continuesFilterChainWhenAuthorizationHeaderIsEmpty() throws ServletException, IOException {
            // Arrange
            request.addHeader(HttpHeaders.AUTHORIZATION, "");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("continues filter chain when Bearer prefix missing")
        void continuesFilterChainWhenBearerPrefixMissing() throws ServletException, IOException {
            // Arrange
            request.addHeader(HttpHeaders.AUTHORIZATION, "some.jwt.token");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("continues filter chain when Bearer token is empty")
        void continuesFilterChainWhenBearerTokenIsEmpty() throws ServletException, IOException {
            // Arrange
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("continues filter chain when Bearer token is whitespace only")
        void continuesFilterChainWhenBearerTokenIsWhitespaceOnly() throws ServletException, IOException {
            // Arrange
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).validateToken(anyString());
        }
    }

    @Nested
    @DisplayName("Invalid token handling")
    class InvalidTokenHandling {

        @Test
        @DisplayName("clears context when token is invalid")
        void clearsContextWhenTokenIsInvalid() throws ServletException, IOException {
            // Arrange
            String invalidToken = "invalid.jwt.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken);

            when(jwtService.validateToken(invalidToken))
                    .thenThrow(InvalidTokenException.malformed(TokenType.ACCESS_TOKEN));

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("clears context when token is expired")
        void clearsContextWhenTokenIsExpired() throws ServletException, IOException {
            // Arrange
            String expiredToken = "expired.jwt.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken);

            when(jwtService.validateToken(expiredToken))
                    .thenThrow(InvalidTokenException.expired(TokenType.ACCESS_TOKEN));

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("clears context when signature is invalid")
        void clearsContextWhenSignatureIsInvalid() throws ServletException, IOException {
            // Arrange
            String badSignatureToken = "bad.signature.token";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + badSignatureToken);

            when(jwtService.validateToken(badSignatureToken))
                    .thenThrow(InvalidTokenException.invalidSignature(TokenType.ACCESS_TOKEN));

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("clears context when wrong algorithm detected")
        void clearsContextWhenWrongAlgorithmDetected() throws ServletException, IOException {
            // Arrange
            String attackToken = "algorithm.confusion.attack";
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + attackToken);

            when(jwtService.validateToken(attackToken))
                    .thenThrow(InvalidTokenException.wrongAlgorithm(TokenType.ACCESS_TOKEN));

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("JwtAuthenticationPrincipal")
    class JwtAuthenticationPrincipalTests {

        @Test
        @DisplayName("getName returns user ID string")
        void getNameReturnsUserIdString() {
            // Arrange
            JwtAuthenticationPrincipal principal =
                    new JwtAuthenticationPrincipal(
                            USER_ID, TENANT_ID, ROLE_ID, BRANCH_ID, EMAIL);

            // Act & Assert
            assertThat(principal.getName()).isEqualTo(USER_ID.toString());
        }

        @Test
        @DisplayName("record components are accessible")
        void recordComponentsAreAccessible() {
            // Arrange
            JwtAuthenticationPrincipal principal =
                    new JwtAuthenticationPrincipal(
                            USER_ID, TENANT_ID, ROLE_ID, BRANCH_ID, EMAIL);

            // Assert
            assertThat(principal.userId()).isEqualTo(USER_ID);
            assertThat(principal.tenantId()).isEqualTo(TENANT_ID);
            assertThat(principal.roleId()).isEqualTo(ROLE_ID);
            assertThat(principal.branchId()).isEqualTo(BRANCH_ID);
            assertThat(principal.email()).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("Integration with real JwtService")
    class IntegrationWithRealJwtService {

        private JwtService realJwtService;
        private RSAPrivateKey privateKey;
        private RSAPublicKey publicKey;

        @BeforeEach
        void setUpRealService() throws NoSuchAlgorithmException {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            privateKey = (RSAPrivateKey) keyPair.getPrivate();
            publicKey = (RSAPublicKey) keyPair.getPublic();

            JwtProperties props = new JwtProperties();
            props.setIssuer("quickstack-pos");
            props.setAccessTokenExpiration(Duration.ofMinutes(15));
            props.setClockSkew(Duration.ofSeconds(30));

            Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
            realJwtService = new JwtService(props, privateKey, publicKey, Collections.emptyList(), fixedClock);
        }

        @Test
        @DisplayName("full flow: generate token, extract, validate, authenticate")
        void fullFlowGenerateValidateAuthenticate() throws ServletException, IOException {
            // Arrange
            User user = new User();
            user.setId(USER_ID);
            user.setTenantId(TENANT_ID);
            user.setRoleId(ROLE_ID);
            user.setBranchId(BRANCH_ID);
            user.setEmail(EMAIL);

            String token = realJwtService.generateAccessToken(user);

            JwtAuthenticationFilter realFilter = new JwtAuthenticationFilter(realJwtService);
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            // Act
            realFilter.doFilterInternal(request, response, filterChain);

            // Assert
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();

            JwtAuthenticationPrincipal principal =
                    (JwtAuthenticationPrincipal) auth.getPrincipal();
            assertThat(principal.userId()).isEqualTo(USER_ID);
            assertThat(principal.tenantId()).isEqualTo(TENANT_ID);
            assertThat(principal.email()).isEqualTo(EMAIL);
        }
    }
}
