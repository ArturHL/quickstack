package com.quickstack.auth.security;

import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.common.exception.InvalidTokenException.TokenType;
import com.quickstack.common.security.SecureTokenGenerator;
import com.quickstack.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating and validating JWT access tokens.
 * <p>
 * Security features:
 * - RS256 signing (asymmetric, 2048+ bit RSA)
 * - Rejects algorithm confusion attacks (only RS256 accepted)
 * - Supports key rotation with previous public keys
 * - Short-lived access tokens (configurable, default 15 min)
 * <p>
 * ASVS Compliance:
 * - V3.5.2: Verifies signature, expiration, and issuer
 * - V3.5.3: Uses RS256 (rejects none, HS256, etc.)
 * - V6.2.1: RSA keys validated at startup (2048+ bits)
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    // Custom claim names
    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_ROLE_ID = "role_id";
    public static final String CLAIM_BRANCH_ID = "branch_id";
    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties properties;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final List<RSAPublicKey> previousPublicKeys;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(
            JwtProperties properties,
            RSAPrivateKey privateKey,
            RSAPublicKey publicKey,
            List<RSAPublicKey> previousPublicKeys) {
        this(properties, privateKey, publicKey, previousPublicKeys, Clock.systemUTC());
    }

    /**
     * Constructor with injectable clock for testing.
     */
    JwtService(
            JwtProperties properties,
            RSAPrivateKey privateKey,
            RSAPublicKey publicKey,
            List<RSAPublicKey> previousPublicKeys,
            Clock clock) {
        this.properties = properties;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.previousPublicKeys = previousPublicKeys;
        this.clock = clock;
    }

    /**
     * Generates an access token for the given user.
     * <p>
     * Token contains the following claims:
     * - sub: user ID (UUID)
     * - email: user email
     * - tenant_id: tenant ID (UUID)
     * - role_id: role ID (UUID)
     * - branch_id: branch ID (UUID, nullable)
     * - jti: unique token ID
     * - iss: issuer
     * - iat: issued at timestamp
     * - exp: expiration timestamp
     *
     * @param user the user to generate token for
     * @return signed JWT access token
     */
    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        Instant expiration = now.plus(properties.getAccessTokenExpiration());
        String tokenId = SecureTokenGenerator.generate();

        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_TENANT_ID, user.getTenantId().toString())
                .claim(CLAIM_ROLE_ID, user.getRoleId().toString())
                .id(tokenId)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey, Jwts.SIG.RS256);

        // Add branch_id only if present
        if (user.getBranchId() != null) {
            builder.claim(CLAIM_BRANCH_ID, user.getBranchId().toString());
        }

        String token = builder.compact();

        log.debug("Generated access token for user {} (tenant: {}, expires: {})",
                user.getId(), user.getTenantId(), expiration);

        return token;
    }

    /**
     * Validates an access token and returns its claims.
     * <p>
     * Validation includes:
     * - Signature verification (RS256 only)
     * - Expiration check (with clock skew tolerance)
     * - Issuer verification
     * <p>
     * If the current key fails, attempts validation with previous keys
     * to support seamless key rotation.
     *
     * @param token the JWT token to validate
     * @return validated claims
     * @throws InvalidTokenException if token is invalid, expired, or has wrong algorithm
     */
    public Claims validateToken(String token) {
        // Try current key first
        try {
            return parseAndValidate(token, publicKey);
        } catch (InvalidTokenException e) {
            // For expired or non-signature errors, don't try previous keys
            if (e.getReason() != InvalidationReason.SIGNATURE_INVALID) {
                throw e;
            }
        }

        // Try previous keys for signature failures (supports key rotation)
        for (int i = 0; i < previousPublicKeys.size(); i++) {
            try {
                Claims claims = parseAndValidate(token, previousPublicKeys.get(i));
                log.warn("Token validated with previous key index {}. Consider rotating tokens soon.", i);
                return claims;
            } catch (InvalidTokenException e) {
                // Continue to next key
                log.debug("Previous key {} failed validation: {}", i, e.getReason());
            }
        }

        // All keys failed
        throw InvalidTokenException.invalidSignature(TokenType.ACCESS_TOKEN);
    }

    /**
     * Extracts the user ID from validated claims.
     *
     * @param claims validated JWT claims
     * @return user UUID
     */
    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the tenant ID from validated claims.
     *
     * @param claims validated JWT claims
     * @return tenant UUID
     */
    public UUID getTenantId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
    }

    /**
     * Extracts the role ID from validated claims.
     *
     * @param claims validated JWT claims
     * @return role UUID
     */
    public UUID getRoleId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_ROLE_ID, String.class));
    }

    /**
     * Extracts the branch ID from validated claims.
     *
     * @param claims validated JWT claims
     * @return branch UUID or null if not present
     */
    public UUID getBranchId(Claims claims) {
        String branchId = claims.get(CLAIM_BRANCH_ID, String.class);
        return branchId != null ? UUID.fromString(branchId) : null;
    }

    /**
     * Extracts the email from validated claims.
     *
     * @param claims validated JWT claims
     * @return user email
     */
    public String getEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * Parses and validates a token with a specific public key.
     */
    private Claims parseAndValidate(String token, RSAPublicKey key) {
        try {
            // JJWT v0.12+ enforces algorithm by default when using verifyWith()
            // It will reject tokens with "none" algorithm or algorithm mismatch
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .clockSkewSeconds(properties.getClockSkew().toSeconds())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw InvalidTokenException.expired(TokenType.ACCESS_TOKEN);

        } catch (UnsupportedJwtException e) {
            // This catches algorithm confusion attacks
            log.warn("Unsupported JWT algorithm detected (possible attack): {}", e.getMessage());
            throw InvalidTokenException.wrongAlgorithm(TokenType.ACCESS_TOKEN);

        } catch (MalformedJwtException e) {
            log.debug("Malformed token: {}", e.getMessage());
            throw InvalidTokenException.malformed(TokenType.ACCESS_TOKEN);

        } catch (SignatureException e) {
            log.debug("Invalid signature: {}", e.getMessage());
            throw InvalidTokenException.invalidSignature(TokenType.ACCESS_TOKEN);

        } catch (io.jsonwebtoken.security.SecurityException e) {
            // Catches weak key attacks and other security issues
            log.warn("JWT security exception: {}", e.getMessage());
            throw new InvalidTokenException(TokenType.ACCESS_TOKEN, InvalidationReason.SIGNATURE_INVALID, e);

        } catch (IllegalArgumentException e) {
            // Empty or null token
            log.debug("Invalid token argument: {}", e.getMessage());
            throw InvalidTokenException.malformed(TokenType.ACCESS_TOKEN);

        } catch (JwtException e) {
            // Catch-all for other JWT exceptions (including issuer mismatch)
            if (e.getMessage() != null && e.getMessage().contains("issuer")) {
                log.debug("Issuer mismatch: {}", e.getMessage());
                throw new InvalidTokenException(TokenType.ACCESS_TOKEN, InvalidationReason.ISSUER_MISMATCH);
            }
            log.debug("JWT exception: {}", e.getMessage());
            throw InvalidTokenException.malformed(TokenType.ACCESS_TOKEN);
        }
    }
}
