package com.quickstack;

import com.quickstack.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check controller (in addition to actuator).
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
            "status", "UP",
            "service", "quickstack-api"
        ));
    }
}
