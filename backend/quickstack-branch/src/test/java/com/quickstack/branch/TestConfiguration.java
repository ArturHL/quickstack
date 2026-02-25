package com.quickstack.branch;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for repository tests.
 * <p>
 * This minimal configuration allows @DataJpaTest to bootstrap the Spring context
 * for repository testing with Testcontainers.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.quickstack.branch")
public class TestConfiguration {
}
