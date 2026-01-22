-- V3: Create Core Domain Tables
-- Tables: organizations, users, invites, waitlist_entries
-- Order: Create organizations first (no dependencies), then users (depends on organizations), 
--        then invites and waitlist_entries (depend on organizations/users)

-- Create organizations table
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    metadata JSONB DEFAULT '{}',
    owner_user_id BIGINT,
    created_by_user_id BIGINT,
    primary_domain VARCHAR(255) UNIQUE,
    use_dedicated_db BOOLEAN NOT NULL DEFAULT FALSE,
    datastore_type VARCHAR(50) NOT NULL DEFAULT 'shared' CHECK (datastore_type IN ('shared', 'dedicated')),
    database_uri TEXT,
    database_name VARCHAR(255),
    data_residency VARCHAR(50) NOT NULL DEFAULT 'shared' CHECK (data_residency IN ('shared', 'dedicated')),
    last_migrated_at TIMESTAMP,
    archived_at TIMESTAMP,
    datastore_history JSONB DEFAULT '[]',
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique partial index on primary_domain (where not null)
CREATE UNIQUE INDEX IF NOT EXISTS idx_org_primary_domain_unique 
    ON organizations(primary_domain) 
    WHERE primary_domain IS NOT NULL;

COMMENT ON TABLE organizations IS 'Tenant organizations';
COMMENT ON COLUMN organizations.name IS 'Unique organization name';
COMMENT ON COLUMN organizations.primary_domain IS 'Primary domain for the organization (unique when not null)';

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'user',
    org_id BIGINT,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token_hash VARCHAR(255),
    verification_token_expires TIMESTAMP,
    reset_token_hash VARCHAR(255),
    reset_token_expires TIMESTAMP,
    archived_at TIMESTAMP,
    last_login TIMESTAMP,
    is_org_owner BOOLEAN NOT NULL DEFAULT FALSE,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- Create user_roles element collection table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for users
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_org_archived ON users(org_id, archived_at);

COMMENT ON TABLE users IS 'User accounts and authentication';
COMMENT ON TABLE user_roles IS 'User roles element collection';

-- Create invites table
CREATE TABLE IF NOT EXISTS invites (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    person_id BIGINT,
    token_hash VARCHAR(255) NOT NULL,
    token_expires TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'expired')),
    created_by_user_id BIGINT NOT NULL,
    invited_user_id BIGINT,
    accepted_at TIMESTAMP,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invite_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invite_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invite_invited_user FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for invites
CREATE INDEX IF NOT EXISTS idx_invite_org_email_status ON invites(org_id, email, status);
CREATE INDEX IF NOT EXISTS idx_invite_org_person_status ON invites(org_id, person_id, status) WHERE person_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invite_token_hash ON invites(token_hash);
CREATE INDEX IF NOT EXISTS idx_invite_token_expires ON invites(token_expires);

COMMENT ON TABLE invites IS 'User invitations';

-- Create waitlist_entries table
CREATE TABLE IF NOT EXISTS waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    source VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'pending-cohort' CHECK (status IN ('pending-cohort', 'invited', 'activated')),
    verify_status VARCHAR(50) NOT NULL DEFAULT 'unverified' CHECK (verify_status IN ('unverified', 'verified', 'blocked')),
    verify_code VARCHAR(255),
    verify_expires_at TIMESTAMP,
    verify_attempts INTEGER NOT NULL DEFAULT 0,
    verify_attempt_total INTEGER NOT NULL DEFAULT 0,
    verify_resends INTEGER NOT NULL DEFAULT 0,
    last_verify_sent_at TIMESTAMP,
    verified_at TIMESTAMP,
    verify_blocked_at TIMESTAMP,
    verify_blocked_until TIMESTAMP,
    phone_verify_status VARCHAR(50) NOT NULL DEFAULT 'unverified' CHECK (phone_verify_status IN ('unverified', 'verified', 'blocked')),
    phone_verify_code VARCHAR(255),
    phone_verify_expires_at TIMESTAMP,
    phone_verify_attempts INTEGER NOT NULL DEFAULT 0,
    phone_verify_attempt_total INTEGER NOT NULL DEFAULT 0,
    phone_verify_resends INTEGER NOT NULL DEFAULT 0,
    phone_last_verify_sent_at TIMESTAMP,
    phone_verified_at TIMESTAMP,
    phone_verify_blocked_at TIMESTAMP,
    phone_verify_blocked_until TIMESTAMP,
    pre_create_account BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    invited_at TIMESTAMP,
    activated_at TIMESTAMP,
    cohort_tag VARCHAR(255),
    metadata JSONB,
    invite_failure_count INTEGER DEFAULT 0,
    invite_token_hash VARCHAR(255),
    invite_token_expires_at TIMESTAMP,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for waitlist_entries
CREATE INDEX IF NOT EXISTS idx_waitlist_email ON waitlist_entries(email);
CREATE INDEX IF NOT EXISTS idx_waitlist_status_created ON waitlist_entries(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_waitlist_verify_status ON waitlist_entries(verify_status, status, created_at);

COMMENT ON TABLE waitlist_entries IS 'Waitlist management';

