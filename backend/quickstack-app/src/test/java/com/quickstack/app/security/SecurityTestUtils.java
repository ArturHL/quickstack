package com.quickstack.app.security;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static utilities for security-focused test assertions.
 * <p>
 * Provides helpers for:
 * <ul>
 *   <li>Measuring execution time (timing oracle detection)</li>
 *   <li>Asserting constant-time behavior between code paths</li>
 * </ul>
 */
public final class SecurityTestUtils {

    private SecurityTestUtils() {}

    /**
     * Measures the execution time of an action in milliseconds.
     * Exceptions thrown by the action are silently swallowed so that
     * timing can be measured regardless of outcome.
     */
    public static long measureMs(Runnable action) {
        long start = System.nanoTime();
        try {
            action.run();
        } catch (Exception ignored) {
            // measure regardless of outcome
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    /**
     * Asserts that two code paths complete within {@code toleranceMs} of each other.
     * <p>
     * Use this to verify that operations that should be constant-time (e.g., user exists
     * vs. user not found in forgot-password flow) do not leak information via timing.
     * <p>
     * Runs each path twice to allow JIT warm-up before measuring.
     *
     * @param pathA       first code path (e.g., email exists)
     * @param pathB       second code path (e.g., email does not exist)
     * @param toleranceMs maximum acceptable timing difference in milliseconds
     */
    public static void assertConstantTime(Runnable pathA, Runnable pathB, long toleranceMs) {
        // JIT warm-up
        measureMs(pathA);
        measureMs(pathB);

        long timeA = measureMs(pathA);
        long timeB = measureMs(pathB);
        long delta = Math.abs(timeA - timeB);

        assertThat(delta)
                .as("Timing difference %dms exceeds tolerance %dms — potential timing oracle "
                    + "(pathA=%dms, pathB=%dms)", delta, toleranceMs, timeA, timeB)
                .isLessThanOrEqualTo(toleranceMs);
    }
}
