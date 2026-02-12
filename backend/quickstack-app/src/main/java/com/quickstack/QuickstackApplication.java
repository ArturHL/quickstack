package com.quickstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.quickstack.common.config.properties")
public class QuickstackApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstackApplication.class, args);
    }
}
