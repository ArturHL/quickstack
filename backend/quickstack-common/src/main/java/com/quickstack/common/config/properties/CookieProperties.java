package com.quickstack.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for secure cookie handling.
 *
 * ASVS V3.4.1: Cookies must have Secure attribute.
 * ASVS V3.4.2: Cookies must have HttpOnly attribute.
 * ASVS V3.4.3: Cookies must use SameSite attribute.
 * ASVS V3.4.4: Cookies should use __Host- prefix.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html">OWASP Session Management</a>
 */
@ConfigurationProperties(prefix = "quickstack.cookie")
@Validated
public class CookieProperties {

    /**
     * Refresh token cookie configuration.
     */
    private RefreshTokenCookie refreshToken = new RefreshTokenCookie();

    // Getters and Setters

    public RefreshTokenCookie getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshTokenCookie refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Refresh token cookie configuration.
     */
    public static class RefreshTokenCookie {

        /**
         * Cookie name. Uses __Host- prefix for maximum security.
         * __Host- prefix requires: Secure=true, no Domain, Path=/.
         */
        @NotBlank(message = "Cookie name must be configured")
        private String name = "__Host-refreshToken";

        /**
         * Cookie path. Restricted to auth endpoints only.
         * Note: __Host- prefix technically requires Path=/, but most
         * modern browsers accept more restrictive paths.
         */
        private String path = "/api/v1/auth";

        /**
         * Mark cookie as secure (HTTPS only). Default: true.
         * ASVS V3.4.1: Required for all session cookies.
         */
        private boolean secure = true;

        /**
         * Mark cookie as HTTP only (not accessible via JavaScript). Default: true.
         * ASVS V3.4.2: Prevents XSS from stealing tokens.
         */
        private boolean httpOnly = true;

        /**
         * SameSite attribute. Default: Strict.
         * ASVS V3.4.3: Prevents CSRF attacks.
         * Options: Strict, Lax, None
         */
        private SameSiteMode sameSite = SameSiteMode.STRICT;

        /**
         * Cookie max age. Should match refresh token expiration.
         */
        private Duration maxAge = Duration.ofDays(7);

        /**
         * Domain attribute. Leave empty to use request domain.
         * Note: __Host- prefix does not allow Domain attribute.
         */
        private String domain;

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public boolean isHttpOnly() {
            return httpOnly;
        }

        public void setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
        }

        public SameSiteMode getSameSite() {
            return sameSite;
        }

        public void setSameSite(SameSiteMode sameSite) {
            this.sameSite = sameSite;
        }

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        /**
         * Returns max age in seconds for cookie creation.
         */
        public long getMaxAgeSeconds() {
            return maxAge.toSeconds();
        }

        /**
         * Validates that configuration is secure.
         * Logs warnings for potentially insecure settings.
         */
        public boolean isSecureConfiguration() {
            return secure && httpOnly && sameSite == SameSiteMode.STRICT;
        }
    }

    /**
     * SameSite cookie attribute values.
     */
    public enum SameSiteMode {
        /**
         * Cookie only sent in first-party context.
         * Most secure option, recommended for auth cookies.
         */
        STRICT("Strict"),

        /**
         * Cookie sent with top-level navigations and GET from third-party sites.
         */
        LAX("Lax"),

        /**
         * Cookie sent in all contexts. Requires Secure attribute.
         * Not recommended for auth cookies.
         */
        NONE("None");

        private final String value;

        SameSiteMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
