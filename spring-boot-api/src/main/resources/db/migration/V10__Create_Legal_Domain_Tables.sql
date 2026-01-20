-- V10: Create Legal Domain Tables
-- Tables: legal_docs, legal_acceptances
-- Order: Create legal_docs first (no dependencies), then legal_acceptances (depends on users, legal_docs)

-- Create legal_docs table
CREATE TABLE IF NOT EXISTS legal_docs (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL CHECK (type IN ('privacy_policy', 'terms')),
    version VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_legal_doc_type_version UNIQUE (type, version)
);

-- Create indexes for legal_docs
CREATE INDEX IF NOT EXISTS idx_legal_doc_type_effective ON legal_docs(type, effective_at DESC, created_at DESC);

COMMENT ON TABLE legal_docs IS 'Legal documents (terms, privacy)';

-- Create legal_acceptances table
CREATE TABLE IF NOT EXISTS legal_acceptances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT,
    doc_type VARCHAR(50) NOT NULL CHECK (doc_type IN ('privacy_policy', 'terms')),
    version VARCHAR(255) NOT NULL,
    accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_legal_acceptance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_legal_acceptance_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE SET NULL,
    CONSTRAINT uk_legal_acceptance_user_doc_version UNIQUE (user_id, doc_type, version)
);

-- Create indexes for legal_acceptances
CREATE INDEX IF NOT EXISTS idx_legal_acceptance_org_doc_version ON legal_acceptances(org_id, doc_type, version);

COMMENT ON TABLE legal_acceptances IS 'User legal document acceptances';

