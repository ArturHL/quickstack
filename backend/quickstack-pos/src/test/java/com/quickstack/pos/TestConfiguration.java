package com.quickstack.pos;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test configuration for repository tests in quickstack-pos.
 * <p>
 * This minimal configuration allows @DataJpaTest to bootstrap the Spring
 * context
 * for repository testing with Testcontainers.
 * <p>
 * Controllers and services are excluded because they depend on beans from other
 * modules (e.g., BranchRepository from quickstack-branch) that are not
 * available
 * in the @DataJpaTest slice. Only entities and repositories are loaded.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.quickstack.pos", excludeFilters = @Filter(type = FilterType.ANNOTATION, classes = {
        Service.class, Controller.class, RestController.class }))
public class TestConfiguration {
}
