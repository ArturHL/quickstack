package com.quickstack.common.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Extracts the real client IP address from HTTP requests.
 *
 * Handles proxy headers (X-Forwarded-For, X-Real-IP) while protecting
 * against header injection attacks.
 *
 * Trust order:
 * 1. X-Forwarded-For (first IP if multiple)
 * 2. X-Real-IP
 * 3. request.getRemoteAddr() (fallback)
 */
public final class IpAddressExtractor {

    /**
     * Standard X-Forwarded-For header.
     */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Nginx X-Real-IP header.
     */
    public static final String HEADER_X_REAL_IP = "X-Real-IP";

    /**
     * Pattern for valid IPv4 address.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
    );

    /**
     * Pattern for valid IPv6 address (simplified).
     */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
            "^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
            "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
            "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
            "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|" +
            "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|" +
            "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|" +
            "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|" +
            "^[0-9a-fA-F]{1,4}:(:[0-9a-fA-F]{1,4}){1,6}$|" +
            "^:((:[0-9a-fA-F]{1,4}){1,7}|:)$"
    );

    /**
     * Maximum length for IP address to prevent DoS via long strings.
     */
    private static final int MAX_IP_LENGTH = 45; // Max IPv6 length

    /**
     * Unknown IP fallback.
     */
    public static final String UNKNOWN_IP = "0.0.0.0";

    private IpAddressExtractor() {
        // Utility class, prevent instantiation
    }

    /**
     * Extracts the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address, or UNKNOWN_IP if not determinable
     */
    public static String extract(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        // Try X-Forwarded-For header first (most common proxy header)
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (isNotBlank(xForwardedFor)) {
            String ip = extractFirstIp(xForwardedFor);
            if (isValidIp(ip)) {
                return sanitize(ip);
            }
        }

        // Try X-Real-IP header (Nginx)
        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (isNotBlank(xRealIp) && isValidIp(xRealIp.trim())) {
            return sanitize(xRealIp.trim());
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        if (isNotBlank(remoteAddr) && isValidIp(remoteAddr)) {
            return sanitize(remoteAddr);
        }

        return UNKNOWN_IP;
    }

    /**
     * Extracts the first IP from X-Forwarded-For header.
     * Format: "client, proxy1, proxy2, ..."
     *
     * @param xForwardedFor the header value
     * @return the first (client) IP
     */
    private static String extractFirstIp(String xForwardedFor) {
        if (xForwardedFor == null) {
            return null;
        }

        // Take first IP (the original client)
        int commaIndex = xForwardedFor.indexOf(',');
        String firstIp = commaIndex > 0
                ? xForwardedFor.substring(0, commaIndex).trim()
                : xForwardedFor.trim();

        return firstIp;
    }

    /**
     * Validates that a string is a valid IP address (IPv4 or IPv6).
     * Protects against header injection attacks.
     *
     * @param ip the IP string to validate
     * @return true if valid IP address
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > MAX_IP_LENGTH) {
            return false;
        }

        String trimmed = ip.trim();
        return IPV4_PATTERN.matcher(trimmed).matches() ||
               IPV6_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Sanitizes an IP address by removing dangerous characters.
     *
     * @param ip the IP to sanitize
     * @return sanitized IP
     */
    private static String sanitize(String ip) {
        if (ip == null) {
            return UNKNOWN_IP;
        }

        // Remove any characters that shouldn't be in an IP
        String sanitized = ip.trim()
                .replaceAll("[^0-9a-fA-F:.%]", "");

        // Enforce max length
        if (sanitized.length() > MAX_IP_LENGTH) {
            sanitized = sanitized.substring(0, MAX_IP_LENGTH);
        }

        return sanitized.isEmpty() ? UNKNOWN_IP : sanitized;
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * Checks if an IP address is a loopback address.
     *
     * @param ip the IP to check
     * @return true if loopback (localhost)
     */
    public static boolean isLoopback(String ip) {
        if (ip == null) {
            return false;
        }
        return ip.equals("127.0.0.1") ||
               ip.equals("::1") ||
               ip.startsWith("127.");
    }

    /**
     * Checks if an IP address is a private/internal address.
     *
     * @param ip the IP to check
     * @return true if private network address
     */
    public static boolean isPrivate(String ip) {
        if (ip == null) {
            return false;
        }

        // IPv4 private ranges
        if (ip.startsWith("10.") ||
            ip.startsWith("192.168.") ||
            ip.startsWith("172.16.") ||
            ip.startsWith("172.17.") ||
            ip.startsWith("172.18.") ||
            ip.startsWith("172.19.") ||
            ip.startsWith("172.20.") ||
            ip.startsWith("172.21.") ||
            ip.startsWith("172.22.") ||
            ip.startsWith("172.23.") ||
            ip.startsWith("172.24.") ||
            ip.startsWith("172.25.") ||
            ip.startsWith("172.26.") ||
            ip.startsWith("172.27.") ||
            ip.startsWith("172.28.") ||
            ip.startsWith("172.29.") ||
            ip.startsWith("172.30.") ||
            ip.startsWith("172.31.")) {
            return true;
        }

        // IPv6 private (fc00::/7 - Unique Local Addresses)
        if (ip.toLowerCase().startsWith("fc") ||
            ip.toLowerCase().startsWith("fd")) {
            return true;
        }

        return isLoopback(ip);
    }
}
