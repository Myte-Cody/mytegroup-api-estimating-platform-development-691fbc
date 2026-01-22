-- V8: Create Cost Domain Tables
-- Tables: cost_codes, cost_code_import_jobs
-- Order: Create cost_code_import_jobs first (depends on organizations), 
--        then cost_codes (depends on organizations and import jobs)

-- Create cost_code_import_jobs table
CREATE TABLE IF NOT EXISTS cost_code_import_jobs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'queued' CHECK (status IN ('queued', 'processing', 'preview', 'done', 'failed')),
    error_message TEXT,
    file_name VARCHAR(255),
    file_mime VARCHAR(255),
    file_size BIGINT,
    file_base64 TEXT,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    resume_requested BOOLEAN NOT NULL DEFAULT TRUE,
    allow_legal_hold_override BOOLEAN NOT NULL DEFAULT FALSE,
    chunk_size INTEGER NOT NULL DEFAULT 100,
    progress JSONB DEFAULT '{}',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cost_code_import_job_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- Create cost_code_import_previews element collection table
CREATE TABLE IF NOT EXISTS cost_code_import_previews (
    cost_code_import_job_id BIGINT NOT NULL,
    category VARCHAR(255),
    code VARCHAR(255),
    description TEXT,
    PRIMARY KEY (cost_code_import_job_id, code),
    CONSTRAINT fk_cost_code_import_previews_job FOREIGN KEY (cost_code_import_job_id) REFERENCES cost_code_import_jobs(id) ON DELETE CASCADE
);

-- Create indexes for cost_code_import_jobs
CREATE INDEX IF NOT EXISTS idx_cost_code_import_job_org ON cost_code_import_jobs(org_id);

COMMENT ON TABLE cost_code_import_jobs IS 'Cost code import jobs';
COMMENT ON TABLE cost_code_import_previews IS 'Cost code import previews element collection';

-- Create cost_codes table
CREATE TABLE IF NOT EXISTS cost_codes (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    category VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    deactivated_at TIMESTAMP,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    import_job_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cost_code_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cost_code_import_job FOREIGN KEY (import_job_id) REFERENCES cost_code_import_jobs(id) ON DELETE SET NULL,
    CONSTRAINT uk_cost_code_org_code UNIQUE (org_id, code)
);

-- Create indexes for cost_codes
CREATE INDEX IF NOT EXISTS idx_cost_code_org_category ON cost_codes(org_id, category);
CREATE INDEX IF NOT EXISTS idx_cost_code_active ON cost_codes(active);
CREATE INDEX IF NOT EXISTS idx_cost_code_import_job ON cost_codes(import_job_id);

COMMENT ON TABLE cost_codes IS 'Cost codes for projects';

