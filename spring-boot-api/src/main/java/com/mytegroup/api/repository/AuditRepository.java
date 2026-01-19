package com.mytegroup.api.repository;

import com.mytegroup.api.entity.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {
    
    List<Audit> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<Audit> findByUsername(String username);
    
    List<Audit> findByStatusCode(Integer statusCode);
    
    List<Audit> findByMethodAndRequestUriContaining(String method, String uriPattern);
    
    List<Audit> findByClientIp(String clientIp);
}

