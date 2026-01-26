package com.mytegroup.api.service.devseed;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.service.organizations.OrganizationsService;
import com.mytegroup.api.service.users.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * Service for development seed data.
 * Automatically seeds development data on application startup (dev environment only).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.dev-seed.enabled", havingValue = "true", matchIfMissing = false)
public class DevSeedService {
    
    private final OrganizationsService organizationsService;
    private final UsersService usersService;
    
    /**
     * Seeds development data on startup
     */
    @PostConstruct
    @Transactional
    public void seed() {
        log.info("Starting development seed...");
        
        try {
            // TODO: Implement seed logic
            // - Create default organization if none exists
            // - Create superadmin user if none exists
            // - Seed taxonomy data
            // - Seed sample entities
            
            log.info("Development seed completed");
        } catch (Exception e) {
            log.error("Development seed failed", e);
        }
    }
}



