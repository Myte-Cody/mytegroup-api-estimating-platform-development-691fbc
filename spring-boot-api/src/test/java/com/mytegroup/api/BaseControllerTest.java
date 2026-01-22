package com.mytegroup.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Abstract base class for controller tests.
 * Provides MockMvc for HTTP request/response testing and mocks all services and utilities.
 * Does NOT use Testcontainers or real database - pure unit testing of controller layer.
 * 
 * Note: Subclasses should use @WebMvcTest with the specific controller class.
 * This base class provides common utilities.
 */
@ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Helper method to convert object to JSON string.
     * @param obj Object to convert
     * @return JSON string
     */
    protected String asJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}

