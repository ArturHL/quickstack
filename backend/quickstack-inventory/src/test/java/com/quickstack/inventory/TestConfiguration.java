package com.quickstack.inventory;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for repository tests in quickstack-inventory.
 * Minimal bootstrap for @DataJpaTest with Testcontainers.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.quickstack.inventory")
public class TestConfiguration {
}
