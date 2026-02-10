/**
 * User module - user management and authentication (OWASP ASVS L2).
 *
 * <p>Includes:
 * <ul>
 *   <li>User entity and repository</li>
 *   <li>Authentication endpoints (login, register, forgot-password, etc.)</li>
 *   <li>JWT token management</li>
 *   <li>Password security (Argon2id, strength validation)</li>
 *   <li>Account protection (rate limiting, lockout)</li>
 * </ul>
 */
package com.quickstack.user;
