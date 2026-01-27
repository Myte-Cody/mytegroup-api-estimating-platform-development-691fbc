package com.mytegroup.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

/**
 * Test configuration that provides mock Redis beans and other necessary test beans.
 * Uses Mockito to create fully functional mocks that allow tests to run without Redis.
 *
 * This configuration is imported by BaseIntegrationTest to provide Redis mocks and other beans.
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * Provides an ObjectMapper for JSON processing in tests.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Provides a mock RedisConnectionFactory.
     * This prevents Spring Data Redis from trying to initialize real connections.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    /**
     * Provides a mock RedisTemplate for string-object operations.
     * SessionsService uses: opsForSet() and opsForValue()
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        
        // Mock the connection factory
        when(template.getConnectionFactory()).thenReturn(connectionFactory);
        
        // Mock SetOperations for SessionsService.registerSession(), removeSession(), etc.
        SetOperations<String, Object> setOps = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(setOps);
        
        // Mock ValueOperations if needed
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        
        return template;
    }

    /**
     * Provides a mock RedisTemplate for string-string operations.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplateString(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        
        // Mock the connection factory
        when(template.getConnectionFactory()).thenReturn(connectionFactory);
        
        // Mock SetOperations
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(template.opsForSet()).thenReturn(setOps);
        
        // Mock ValueOperations if needed
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        
        return template;
    }
}

