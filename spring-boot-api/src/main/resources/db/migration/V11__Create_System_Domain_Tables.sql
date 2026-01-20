-- V11: Create System Domain Tables
-- Tables: event_logs, tenant_migrations
-- Order: Both depend on organizations, create in any order

-- Create event_logs table
CREATE TABLE IF NOT EXISTS event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    action VARCHAR(255),
    entity VARCHAR(255),
    entity_type VARCHAR(255),
    entity_id VARCHAR(255),
    user_id VARCHAR(255),
    actor VARCHAR(255),
    org_id BIGINT,
    payload JSONB DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    ip_address VARCHAR(45),
    session_id VARCHAR(255),
    request_id VARCHAR(255),
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_log_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE SET NULL
);

-- Create indexes for event_logs
CREATE INDEX IF NOT EXISTS idx_event_log_event_type ON event_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_event_log_action ON event_logs(action);
CREATE INDEX IF NOT EXISTS idx_event_log_entity_type ON event_logs(entity_type);
CREATE INDEX IF NOT EXISTS idx_event_log_entity_id ON event_logs(entity_id);
CREATE INDEX IF NOT EXISTS idx_event_log_actor ON event_logs(actor);
CREATE INDEX IF NOT EXISTS idx_event_log_org_created ON event_logs(org_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_log_org_entity_created ON event_logs(org_id, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_log_org_action_created ON event_logs(org_id, action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_event_log_org_event_type_created ON event_logs(org_id, event_type, created_at DESC);

-- Note: TTL index on created_at (10 years retention, skip when legal_hold is true)
-- This will be handled via application logic or PostgreSQL features
-- For now, we'll rely on application-level cleanup

COMMENT ON TABLE event_logs IS 'System event logging';

-- Create tenant_migrations table
CREATE TABLE IF NOT EXISTS tenant_migrations (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    direction VARCHAR(50) NOT NULL CHECK (direction IN ('shared_to_dedicated', 'dedicated_to_shared')),
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'in_progress', 'ready_for_cutover', 'completed', 'failed', 'aborted')),
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    resume_requested BOOLEAN NOT NULL DEFAULT TRUE,
    allow_legal_hold_override BOOLEAN NOT NULL DEFAULT FALSE,
    actor_user_id VARCHAR(255),
    actor_role VARCHAR(255),
    target_uri TEXT,
    target_db_name VARCHAR(255),
    chunk_size INTEGER NOT NULL DEFAULT 100,
    progress JSONB DEFAULT '{}',
    error TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    last_progress_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_migration_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- Create indexes for tenant_migrations
CREATE INDEX IF NOT EXISTS idx_tenant_migration_org ON tenant_migrations(org_id);

COMMENT ON TABLE tenant_migrations IS 'Database migration tracking';

