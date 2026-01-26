package com.mytegroup.api.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytegroup.api.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract base class for all controller integration tests.
 * 
 * Provides common setup and utilities for testing controllers with:
 * - Role-Based Access Control (RBAC) verification
 * - Request/response validation
 * - JSON response structure checking
 * - Common HTTP methods and status codes
 * 
 * Extends BaseIntegrationTest to provide:
 * - Database access via autowired repositories
 * - Testcontainers PostgreSQL setup
 * - Automatic database cleanup between tests
 * - Test data management
 * 
 * Usage Example:
 * <pre>
 * @SpringBootTest
 * @ActiveProfiles("test")
 * class CompanyControllerTest extends BaseControllerTest {
 *     
 *     private Organization testOrg;
 *     
 *     @BeforeEach
 *     void setUp() {
 *         super.setUp();
 *         testOrg = organizationRepository.save(new Organization());
 *     }
 *     
 *     @Test
 *     @WithMockUser(roles = "ORG_ADMIN")
 *     void testListCompanies_WithOrgAdminRole_IsAllowed() throws Exception {
 *         mockMvc.perform(get("/api/companies")
 *             .param("orgId", testOrg.getId().toString()))
 *             .andExpect(status().isOk());
 *     }
 *     
 *     @Test
 *     @WithMockUser(roles = "USER")
 *     void testListCompanies_WithUserRole_IsForbidden() throws Exception {
 *         mockMvc.perform(get("/api/companies")
 *             .param("orgId", testOrg.getId().toString()))
 *             .andExpect(status().isForbidden());
 *     }
 * }
 * </pre>
 */
public abstract class BaseControllerTest extends BaseIntegrationTest {

    /**
     * WebApplicationContext for MockMvc setup
     */
    @Autowired
    protected WebApplicationContext applicationContext;

    /**
     * ObjectMapper for JSON serialization/deserialization in tests
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * MockMvc instance for performing HTTP requests
     */
    protected MockMvc mockMvc;

    /**
     * Setup method to initialize MockMvc.
     * Must be called in @BeforeEach method of subclass.
     * 
     * Example:
     * <pre>
     * @BeforeEach
     * void setUp() {
     *     super.setUp();
     *     // Additional setup specific to test class
     * }
     * </pre>
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    // ========== ROLE-BASED ACCESS CONTROL CONSTANTS ==========

    /**
     * Admin roles that typically have full access to protected endpoints
     */
    public static final String[] ADMIN_ROLES = {
            "ORG_OWNER",       // Organization owner
            "ORG_ADMIN",       // Organization admin
            "ADMIN",           // Platform admin
            "SUPER_ADMIN",     // Super admin
            "PLATFORM_ADMIN"   // Platform admin
    };

    /**
     * User roles that typically have limited or no access to admin endpoints
     */
    public static final String[] USER_ROLES = {
            "USER",     // Regular user
            "GUEST"     // Guest user
    };

    /**
     * Individual role constants for use in @WithMockUser annotations
     */
    public static final String ROLE_ORG_OWNER = "ORG_OWNER";
    public static final String ROLE_ORG_ADMIN = "ORG_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_GUEST = "GUEST";

    // ========== HTTP METHOD CONSTANTS ==========

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String PATCH = "PATCH";
    public static final String DELETE = "DELETE";

    // ========== HTTP STATUS CODE CONSTANTS ==========

    /**
     * HTTP status code constants for common responses
     */
    public static final class HttpStatus {
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int NO_CONTENT = 204;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int CONFLICT = 409;
        public static final int UNPROCESSABLE_ENTITY = 422;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int SERVICE_UNAVAILABLE = 503;
    }

    // ========== CONTENT TYPE CONSTANTS ==========

    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String TEXT_PLAIN = "text/plain";

    // ========== RBAC TEST HELPER METHODS ==========

    /**
     * Get all admin roles for parameterized testing
     * 
     * @return Array of admin role strings
     */
    protected String[] getAdminRoles() {
        return ADMIN_ROLES;
    }

    /**
     * Get all user roles for parameterized testing
     * 
     * @return Array of user role strings
     */
    protected String[] getUserRoles() {
        return USER_ROLES;
    }

    /**
     * Check if a role is an admin role
     * 
     * @param role Role to check
     * @return true if role is admin, false otherwise
     */
    protected boolean isAdminRole(String role) {
        for (String adminRole : ADMIN_ROLES) {
            if (adminRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a role is a user role
     * 
     * @param role Role to check
     * @return true if role is user, false otherwise
     */
    protected boolean isUserRole(String role) {
        for (String userRole : USER_ROLES) {
            if (userRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    // ========== COMMON TEST UTILITIES ==========

    /**
     * Convert object to JSON string for request bodies
     * 
     * @param object Object to convert
     * @return JSON string representation
     * @throws Exception if serialization fails
     */
    protected String asJsonString(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Get MockMvc instance for making HTTP requests
     * 
     * @return MockMvc instance
     */
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    /**
     * Get ObjectMapper for manual JSON operations
     * 
     * @return ObjectMapper instance
     */
    protected ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }
}



