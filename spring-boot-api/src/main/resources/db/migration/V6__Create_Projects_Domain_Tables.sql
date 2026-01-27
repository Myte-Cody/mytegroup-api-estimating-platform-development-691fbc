-- V6: Create Projects Domain Tables
-- Tables: projects, estimates, seats
-- Order: Create projects first (depends on organizations, offices), 
--        then estimates (depends on projects), then seats (depends on organizations, projects, users)

-- Create projects table
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    org_id BIGINT NOT NULL,
    office_id BIGINT,
    description TEXT,
    project_code VARCHAR(255),
    status VARCHAR(255),
    location VARCHAR(255),
    bid_date DATE,
    award_date DATE,
    fabrication_start_date DATE,
    fabrication_end_date DATE,
    erection_start_date DATE,
    erection_end_date DATE,
    completion_date DATE,
    -- ProjectBudget embedded fields
    hours DOUBLE PRECISION,
    labour_rate DOUBLE PRECISION,
    currency VARCHAR(50),
    amount DOUBLE PRECISION,
    -- ProjectQuantities embedded fields (JSONB)
    structural JSONB,
    misc_metals JSONB,
    metal_deck JSONB,
    clt_panels JSONB,
    glulam JSONB,
    -- ProjectStaffing embedded fields
    project_manager_person_id BIGINT,
    foreman_person_ids JSONB,
    superintendent_person_id BIGINT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_office FOREIGN KEY (office_id) REFERENCES offices(id) ON DELETE SET NULL
);

-- Create project_cost_code_budgets element collection table
CREATE TABLE IF NOT EXISTS project_cost_code_budgets (
    project_id BIGINT NOT NULL,
    cost_code_id BIGINT NOT NULL,
    budgeted_hours DOUBLE PRECISION,
    cost_budget DOUBLE PRECISION,
    PRIMARY KEY (project_id, cost_code_id),
    CONSTRAINT fk_project_cost_code_budgets_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Create project_seat_assignments element collection table
CREATE TABLE IF NOT EXISTS project_seat_assignments (
    project_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    person_id BIGINT,
    role VARCHAR(255),
    assigned_at TIMESTAMP,
    removed_at TIMESTAMP,
    PRIMARY KEY (project_id, seat_id),
    CONSTRAINT fk_project_seat_assignments_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Create indexes for projects
CREATE UNIQUE INDEX IF NOT EXISTS idx_project_org_name 
    ON projects(org_id, name) 
    WHERE archived_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_project_org_code 
    ON projects(org_id, project_code) 
    WHERE archived_at IS NULL AND project_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_project_org_archived ON projects(org_id, archived_at);

COMMENT ON TABLE projects IS 'Construction projects';
COMMENT ON TABLE project_cost_code_budgets IS 'Project cost code budgets element collection';
COMMENT ON TABLE project_seat_assignments IS 'Project seat assignments element collection';

-- Create estimates table
CREATE TABLE IF NOT EXISTS estimates (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'final', 'archived')),
    total_amount DOUBLE PRECISION,
    revision INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_estimate_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_estimate_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_estimate_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create estimate_line_items element collection table
CREATE TABLE IF NOT EXISTS estimate_line_items (
    estimate_id BIGINT NOT NULL,
    code VARCHAR(255),
    description TEXT,
    quantity DOUBLE PRECISION,
    unit VARCHAR(255),
    unit_cost DOUBLE PRECISION,
    total DOUBLE PRECISION,
    PRIMARY KEY (estimate_id, code),
    CONSTRAINT fk_estimate_line_items_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id) ON DELETE CASCADE
);

-- Create indexes for estimates
CREATE UNIQUE INDEX IF NOT EXISTS idx_estimate_org_project_name 
    ON estimates(org_id, project_id, name) 
    WHERE archived_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_estimate_org_project_archived ON estimates(org_id, project_id, archived_at);

COMMENT ON TABLE estimates IS 'Project estimates';
COMMENT ON TABLE estimate_line_items IS 'Estimate line items element collection';

-- Create seats table
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    seat_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'vacant' CHECK (status IN ('vacant', 'active')),
    role VARCHAR(255),
    user_id BIGINT,
    project_id BIGINT,
    activated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seat_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_seat_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_seat_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT uk_seat_org_seat_number UNIQUE (org_id, seat_number)
);

-- Create seat_history element collection table
CREATE TABLE IF NOT EXISTS seat_history (
    seat_id BIGINT NOT NULL,
    user_id BIGINT,
    project_id BIGINT,
    role VARCHAR(255),
    assigned_at TIMESTAMP,
    removed_at TIMESTAMP,
    PRIMARY KEY (seat_id, assigned_at),
    CONSTRAINT fk_seat_history_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE
);

-- Create indexes for seats
CREATE UNIQUE INDEX IF NOT EXISTS idx_seat_org_user 
    ON seats(org_id, user_id) 
    WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_seat_org_status ON seats(org_id, status);
CREATE INDEX IF NOT EXISTS idx_seat_org_role_status ON seats(org_id, role, status);
CREATE INDEX IF NOT EXISTS idx_seat_org_project ON seats(org_id, project_id);

COMMENT ON TABLE seats IS 'Seat assignments for projects';
COMMENT ON TABLE seat_history IS 'Seat history element collection';

