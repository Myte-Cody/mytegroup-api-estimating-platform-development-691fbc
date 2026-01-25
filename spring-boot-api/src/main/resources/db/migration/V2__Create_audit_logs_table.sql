-- Create audit_logs table for request auditing
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_uri VARCHAR(2048) NOT NULL,
    query_string VARCHAR(2048),
    status_code INTEGER NOT NULL,
    duration_ms BIGINT,
    client_ip VARCHAR(45),
    username VARCHAR(255),
    user_agent VARCHAR(512),
    request_size BIGINT,
    response_size BIGINT,
    error_message VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_method_uri ON audit_logs(method, request_uri);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_logs(status_code);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(username);
CREATE INDEX IF NOT EXISTS idx_audit_client_ip ON audit_logs(client_ip);

-- Create index for date range queries
CREATE INDEX IF NOT EXISTS idx_audit_timestamp_desc ON audit_logs(timestamp DESC);

COMMENT ON TABLE audit_logs IS 'Audit log table for tracking all HTTP requests';
COMMENT ON COLUMN audit_logs.timestamp IS 'Timestamp when the request was processed';
COMMENT ON COLUMN audit_logs.method IS 'HTTP method (GET, POST, PUT, DELETE, etc.)';
COMMENT ON COLUMN audit_logs.request_uri IS 'Request URI path';
COMMENT ON COLUMN audit_logs.query_string IS 'Query string parameters';
COMMENT ON COLUMN audit_logs.status_code IS 'HTTP response status code';
COMMENT ON COLUMN audit_logs.duration_ms IS 'Request processing duration in milliseconds';
COMMENT ON COLUMN audit_logs.client_ip IS 'Client IP address';
COMMENT ON COLUMN audit_logs.username IS 'Authenticated username (if available)';
COMMENT ON COLUMN audit_logs.user_agent IS 'User agent string';
COMMENT ON COLUMN audit_logs.request_size IS 'Request body size in bytes';
COMMENT ON COLUMN audit_logs.response_size IS 'Response body size in bytes';
COMMENT ON COLUMN audit_logs.error_message IS 'Error message if request failed';


