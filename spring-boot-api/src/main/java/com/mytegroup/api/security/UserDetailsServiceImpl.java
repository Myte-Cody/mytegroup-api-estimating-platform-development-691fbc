package com.mytegroup.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Placeholder implementation of UserDetailsService.
 * This should be replaced with actual user repository implementation during migration.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Replace with actual user repository lookup
        // For now, throw exception as this needs to be implemented with actual user entity
        throw new UsernameNotFoundException("User not found: " + username);
    }
}

