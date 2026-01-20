-- V7: Create Companies Domain Tables
-- Tables: companies, company_locations
-- Order: Create companies first (depends on organizations), then company_locations (depends on companies)

-- Create companies table
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    external_id VARCHAR(255),
    website VARCHAR(255),
    main_email VARCHAR(255),
    main_phone VARCHAR(255),
    rating DOUBLE PRECISION,
    notes TEXT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- Create company_type_keys element collection table
CREATE TABLE IF NOT EXISTS company_type_keys (
    company_id BIGINT NOT NULL,
    type_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (company_id, type_key),
    CONSTRAINT fk_company_type_keys_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Create company_tag_keys element collection table
CREATE TABLE IF NOT EXISTS company_tag_keys (
    company_id BIGINT NOT NULL,
    tag_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (company_id, tag_key),
    CONSTRAINT fk_company_tag_keys_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Create indexes for companies
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_org_normalized_name 
    ON companies(org_id, normalized_name) 
    WHERE archived_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_org_external_id 
    ON companies(org_id, external_id) 
    WHERE archived_at IS NULL AND external_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_company_org_archived ON companies(org_id, archived_at);

COMMENT ON TABLE companies IS 'External companies';
COMMENT ON TABLE company_type_keys IS 'Company type keys element collection';
COMMENT ON TABLE company_tag_keys IS 'Company tag keys element collection';

-- Create company_locations table
CREATE TABLE IF NOT EXISTS company_locations (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    external_id VARCHAR(255),
    timezone VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(255),
    region VARCHAR(255),
    postal VARCHAR(255),
    country VARCHAR(255),
    notes TEXT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_location_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_company_location_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT
);

-- Create company_location_tag_keys element collection table
CREATE TABLE IF NOT EXISTS company_location_tag_keys (
    company_location_id BIGINT NOT NULL,
    tag_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (company_location_id, tag_key),
    CONSTRAINT fk_company_location_tag_keys_location FOREIGN KEY (company_location_id) REFERENCES company_locations(id) ON DELETE CASCADE
);

-- Create indexes for company_locations
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_location_org_company_normalized 
    ON company_locations(org_id, company_id, normalized_name) 
    WHERE archived_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_location_org_company_external 
    ON company_locations(org_id, company_id, external_id) 
    WHERE archived_at IS NULL AND external_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_company_location_org_company ON company_locations(org_id, company_id);
CREATE INDEX IF NOT EXISTS idx_company_location_org_archived ON company_locations(org_id, archived_at);

COMMENT ON TABLE company_locations IS 'Company locations';
COMMENT ON TABLE company_location_tag_keys IS 'Company location tag keys element collection';

-- Add foreign keys from persons to companies and company_locations
ALTER TABLE persons 
    ADD CONSTRAINT fk_person_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_person_company_location FOREIGN KEY (company_location_id) REFERENCES company_locations(id) ON DELETE SET NULL;

-- Add foreign key from persons to offices
ALTER TABLE persons 
    ADD CONSTRAINT fk_person_org_location FOREIGN KEY (org_location_id) REFERENCES offices(id) ON DELETE SET NULL;

-- Add foreign key from contacts to offices
ALTER TABLE contacts 
    ADD CONSTRAINT fk_contact_office FOREIGN KEY (office_id) REFERENCES offices(id) ON DELETE SET NULL;

