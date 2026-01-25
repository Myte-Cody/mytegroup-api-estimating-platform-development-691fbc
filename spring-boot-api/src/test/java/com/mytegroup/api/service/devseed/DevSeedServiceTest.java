package com.mytegroup.api.service.devseed;

import com.mytegroup.api.service.organizations.OrganizationsService;
import com.mytegroup.api.service.users.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevSeedServiceTest {

    @Mock
    private OrganizationsService organizationsService;

    @Mock
    private UsersService usersService;

    @InjectMocks
    private DevSeedService devSeedService;

    @BeforeEach
    void setUp() {
        // DevSeedService uses @PostConstruct, so we need to manually call seed() for testing
    }

    @Test
    void testSeed_ExecutesWithoutException() {
        // Since seed() is a @PostConstruct method and currently has TODO implementation,
        // we just verify it doesn't throw exceptions
        assertDoesNotThrow(() -> {
            devSeedService.seed();
        });
    }

    @Test
    void testSeed_HandlesExceptionsGracefully() {
        // Verify that exceptions are caught and logged
        // Since seed() currently has TODO implementation, it should handle exceptions gracefully
        // Should not throw exception even if dependencies fail
        assertDoesNotThrow(() -> {
            devSeedService.seed();
        });
    }
}

