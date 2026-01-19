package com.mytegroup.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret = "your-secret-key-change-in-production-use-environment-variable";
    private long expiration = 86400000; // 24 hours in milliseconds
    private String header = "Authorization";
    private String prefix = "Bearer ";
}

