package com.mytegroup.api.filter;

import com.mytegroup.api.entity.Audit;
import com.mytegroup.api.repository.AuditRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final AuditRepository auditRepository;

    private static final List<String> SENSITIVE_HEADERS = List.of(
            "authorization", "cookie", "x-api-key", "x-auth-token"
    );

    private static final List<String> SENSITIVE_PARAMS = List.of(
            "password", "token", "secret", "apiKey", "accessToken", "refreshToken"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        // Wrap request and response to allow reading body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String errorMessage = null;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(wrappedRequest, wrappedResponse, duration);
            saveAuditRecord(wrappedRequest, wrappedResponse, duration, errorMessage);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long duration) {
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;
        int status = response.getStatus();
        String clientIp = getClientIpAddress(request);
        
        // Log basic request info
        log.info("Request: {} {} | Status: {} | Duration: {}ms | IP: {}",
                method, fullUrl, status, duration, clientIp);
        
        // Log request headers (sanitized)
        if (log.isDebugEnabled()) {
            log.debug("Request Headers: {}", sanitizeHeaders(request));
        }
        
        // Log request parameters (sanitized)
        if (log.isDebugEnabled() && request.getParameterMap() != null && !request.getParameterMap().isEmpty()) {
            log.debug("Request Parameters: {}", sanitizeParameters(request));
        }
        
        // Log request body for POST/PUT/PATCH requests
        if (log.isDebugEnabled() && isRequestBodyLoggable(method)) {
            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                log.debug("Request Body: {}", sanitizeBody(requestBody));
            }
        }
        
        // Log response headers
        if (log.isDebugEnabled()) {
            log.debug("Response Headers: {}", getResponseHeaders(response));
        }
        
        // Log response body for errors
        if (log.isDebugEnabled() && status >= 400) {
            String responseBody = getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                log.debug("Response Body: {}", responseBody);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String sanitizeHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> {
                            String value = request.getHeader(name);
                            if (SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                                return maskSensitiveValue(value);
                            }
                            return value;
                        }
                ))
                .toString();
    }

    private String sanitizeParameters(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> {
                            String[] values = entry.getValue();
                            if (SENSITIVE_PARAMS.contains(entry.getKey().toLowerCase())) {
                                return maskSensitiveValue(values.length > 0 ? values[0] : "");
                            }
                            return String.join(", ", values);
                        }
                ))
                .toString();
    }

    private String sanitizeBody(String body) {
        // Simple sanitization - mask common sensitive fields in JSON
        String sanitized = body;
        for (String sensitiveParam : SENSITIVE_PARAMS) {
            sanitized = sanitized.replaceAll(
                    "(?i)\"" + sensitiveParam + "\"\\s*:\\s*\"[^\"]*\"",
                    "\"" + sensitiveParam + "\": \"***MASKED***\""
            );
        }
        return sanitized;
    }

    private String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    private boolean isRequestBodyLoggable(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] contentAsByteArray = request.getContentAsByteArray();
        if (contentAsByteArray.length == 0) {
            return null;
        }
        try {
            return new String(contentAsByteArray, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to read request body: {}", e.getMessage());
            return null;
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] contentAsByteArray = response.getContentAsByteArray();
        if (contentAsByteArray.length == 0) {
            return null;
        }
        try {
            return new String(contentAsByteArray, response.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to read response body: {}", e.getMessage());
            return null;
        }
    }

    private String getResponseHeaders(ContentCachingResponseWrapper response) {
        return response.getHeaderNames()
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        response::getHeader
                ))
                .toString();
    }

    private void saveAuditRecord(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long duration,
            String errorMessage) {
        
        try {
            String username = getUsername();
            
            Audit audit = Audit.builder()
                    .timestamp(LocalDateTime.now())
                    .method(request.getMethod())
                    .requestUri(request.getRequestURI())
                    .queryString(request.getQueryString())
                    .statusCode(response.getStatus())
                    .durationMs(duration)
                    .clientIp(getClientIpAddress(request))
                    .username(username)
                    .userAgent(request.getHeader("User-Agent"))
                    .requestSize((long) request.getContentAsByteArray().length)
                    .responseSize((long) response.getContentAsByteArray().length)
                    .errorMessage(errorMessage)
                    .build();
            
            auditRepository.save(audit);
        } catch (Exception e) {
            // Log error but don't fail the request if audit saving fails
            log.error("Failed to save audit record: {}", e.getMessage(), e);
        }
    }

    private String getUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not extract username from security context: {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for actuator endpoints to reduce noise
        String path = request.getRequestURI();
        return path.startsWith("/actuator") && !path.equals("/actuator/health");
    }
}

