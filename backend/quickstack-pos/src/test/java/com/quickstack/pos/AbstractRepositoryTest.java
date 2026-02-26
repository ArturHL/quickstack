package com.quickstack.pos;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base for all @DataJpaTest classes in quickstack-pos.
 * <p>
 * Starts a single PostgreSQL container once (static initializer) and provides
 * it to all subclasses via @DynamicPropertySource.
 * <p>
 * Enable container reuse across JVM restarts (local dev) by adding
 * {@code testcontainers.reuse.enable=true} to {@code ~/.testcontainers.properties}.
 */
public abstract class AbstractRepositoryTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quickstack_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
