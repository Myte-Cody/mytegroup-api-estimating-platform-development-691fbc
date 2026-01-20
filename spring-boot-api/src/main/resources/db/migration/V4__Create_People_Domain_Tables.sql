-- V4: Create People Domain Tables
-- Tables: persons, contacts
-- Order: Create persons and contacts (both depend on organizations)

-- Create persons table
CREATE TABLE IF NOT EXISTS persons (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    external_id VARCHAR(255),
    person_type VARCHAR(50) NOT NULL CHECK (person_type IN ('internal_staff', 'internal_union', 'external_person')),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    display_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    primary_email VARCHAR(255),
    primary_phone_e164 VARCHAR(255),
    department_key VARCHAR(255),
    org_location_id BIGINT,
    reports_to_person_id BIGINT,
    ironworker_number VARCHAR(255),
    union_local VARCHAR(255),
    rating DOUBLE PRECISION,
    notes TEXT,
    company_id BIGINT,
    company_location_id BIGINT,
    title VARCHAR(255),
    user_id BIGINT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_person_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_person_reports_to FOREIGN KEY (reports_to_person_id) REFERENCES persons(id) ON DELETE SET NULL,
    CONSTRAINT fk_person_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create person_emails element collection table
CREATE TABLE IF NOT EXISTS person_emails (
    person_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    normalized VARCHAR(255) NOT NULL,
    label VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    PRIMARY KEY (person_id, normalized),
    CONSTRAINT fk_person_emails_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create person_phones element collection table
CREATE TABLE IF NOT EXISTS person_phones (
    person_id BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    e164 VARCHAR(255) NOT NULL,
    label VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (person_id, e164),
    CONSTRAINT fk_person_phones_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create person_certifications element collection table
CREATE TABLE IF NOT EXISTS person_certifications (
    person_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP,
    expires_at TIMESTAMP,
    document_url TEXT,
    notes TEXT,
    PRIMARY KEY (person_id, name),
    CONSTRAINT fk_person_certifications_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create person_tag_keys element collection table
CREATE TABLE IF NOT EXISTS person_tag_keys (
    person_id BIGINT NOT NULL,
    tag_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (person_id, tag_key),
    CONSTRAINT fk_person_tag_keys_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create person_skill_keys element collection table
CREATE TABLE IF NOT EXISTS person_skill_keys (
    person_id BIGINT NOT NULL,
    skill_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (person_id, skill_key),
    CONSTRAINT fk_person_skill_keys_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create person_skill_free_text element collection table
CREATE TABLE IF NOT EXISTS person_skill_free_text (
    person_id BIGINT NOT NULL,
    skill_text VARCHAR(255) NOT NULL,
    PRIMARY KEY (person_id, skill_text),
    CONSTRAINT fk_person_skill_free_text_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- Create indexes for persons
CREATE UNIQUE INDEX IF NOT EXISTS idx_person_org_primary_email 
    ON persons(org_id, primary_email) 
    WHERE archived_at IS NULL AND primary_email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_person_org_external_id 
    ON persons(org_id, external_id) 
    WHERE archived_at IS NULL AND external_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_person_org_primary_phone 
    ON persons(org_id, primary_phone_e164) 
    WHERE archived_at IS NULL AND primary_phone_e164 IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_person_org_ironworker 
    ON persons(org_id, ironworker_number) 
    WHERE archived_at IS NULL AND ironworker_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_person_org_archived ON persons(org_id, archived_at);
CREATE INDEX IF NOT EXISTS idx_person_org_location ON persons(org_id, org_location_id);
CREATE INDEX IF NOT EXISTS idx_person_reports_to ON persons(org_id, reports_to_person_id);
CREATE INDEX IF NOT EXISTS idx_person_company ON persons(org_id, company_id);
CREATE INDEX IF NOT EXISTS idx_person_user ON persons(org_id, user_id);

-- Create indexes for person_emails
CREATE INDEX IF NOT EXISTS idx_person_emails_normalized ON person_emails(normalized);

-- Create indexes for person_phones
CREATE INDEX IF NOT EXISTS idx_person_phones_e164 ON person_phones(e164);

COMMENT ON TABLE persons IS 'Modern person entity with emails/phones arrays';
COMMENT ON TABLE person_emails IS 'Person emails element collection';
COMMENT ON TABLE person_phones IS 'Person phones element collection';
COMMENT ON TABLE person_certifications IS 'Person certifications element collection';

-- Create contacts table
CREATE TABLE IF NOT EXISTS contacts (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    person_type VARCHAR(50) NOT NULL DEFAULT 'external' CHECK (person_type IN ('staff', 'ironworker', 'external')),
    contact_kind VARCHAR(50) NOT NULL DEFAULT 'individual' CHECK (contact_kind IN ('individual', 'business')),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    display_name VARCHAR(255),
    date_of_birth DATE,
    ironworker_number VARCHAR(255),
    union_local VARCHAR(255),
    promoted_to_foreman BOOLEAN NOT NULL DEFAULT FALSE,
    foreman_user_id BIGINT,
    office_id BIGINT,
    reports_to_contact_id BIGINT,
    rating DOUBLE PRECISION,
    email VARCHAR(255),
    phone VARCHAR(255),
    company VARCHAR(255),
    notes TEXT,
    invited_user_id BIGINT,
    invited_at TIMESTAMP,
    invite_status VARCHAR(50),
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contact_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_contact_foreman_user FOREIGN KEY (foreman_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_contact_invited_user FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_contact_reports_to FOREIGN KEY (reports_to_contact_id) REFERENCES contacts(id) ON DELETE SET NULL
);

-- Create contact_certifications element collection table
CREATE TABLE IF NOT EXISTS contact_certifications (
    contact_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP,
    expires_at TIMESTAMP,
    document_url TEXT,
    notes TEXT,
    PRIMARY KEY (contact_id, name),
    CONSTRAINT fk_contact_certifications_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Create contact_roles element collection table
CREATE TABLE IF NOT EXISTS contact_roles (
    contact_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (contact_id, role),
    CONSTRAINT fk_contact_roles_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Create contact_tags element collection table
CREATE TABLE IF NOT EXISTS contact_tags (
    contact_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (contact_id, tag),
    CONSTRAINT fk_contact_tags_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Create contact_skills element collection table
CREATE TABLE IF NOT EXISTS contact_skills (
    contact_id BIGINT NOT NULL,
    skill VARCHAR(255) NOT NULL,
    PRIMARY KEY (contact_id, skill),
    CONSTRAINT fk_contact_skills_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Create indexes for contacts
CREATE INDEX IF NOT EXISTS idx_contact_org_email ON contacts(org_id, email);
CREATE INDEX IF NOT EXISTS idx_contact_org_person_type ON contacts(org_id, person_type, archived_at);
CREATE INDEX IF NOT EXISTS idx_contact_org_ironworker ON contacts(org_id, ironworker_number);
CREATE INDEX IF NOT EXISTS idx_contact_org_archived ON contacts(org_id, archived_at);
CREATE INDEX IF NOT EXISTS idx_contact_org_location ON contacts(org_id, office_id);
CREATE INDEX IF NOT EXISTS idx_contact_reports_to ON contacts(org_id, reports_to_contact_id);

COMMENT ON TABLE contacts IS 'Legacy contact entity';
COMMENT ON TABLE contact_certifications IS 'Contact certifications element collection';

-- Add foreign key from invites to persons (created after persons table)
ALTER TABLE invites 
    ADD CONSTRAINT fk_invite_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE SET NULL;

