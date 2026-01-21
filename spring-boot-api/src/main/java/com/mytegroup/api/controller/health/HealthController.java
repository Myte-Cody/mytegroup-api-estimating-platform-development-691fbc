package com.mytegroup.api.controller.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "timestamp", LocalDateTime.now().toString(),
            "service", "mytegroup-api"
        );
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        return Map.of(
            "status", "ready",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/live")
    public Map<String, Object> live() {
        return Map.of(
            "status", "live",
            "timestamp", LocalDateTime.now().toString()
        );
    }
}
