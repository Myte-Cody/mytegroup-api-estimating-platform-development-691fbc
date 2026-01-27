-- V12: Create Phase 6 Timesheets and Crew Management Tables
-- Tables: timesheets, timesheet_entries, crew_assignments, crew_swaps

-- Create timesheets table
CREATE TABLE IF NOT EXISTS timesheets (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    crew_id VARCHAR(255),
    work_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255),
    submitted_at TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    rejected_by VARCHAR(255),
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timesheets_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_timesheets_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_timesheets_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_timesheet_org_project_person_date_active
    ON timesheets(org_id, project_id, person_id, work_date)
    WHERE archived_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_timesheet_org_status ON timesheets(org_id, status);
CREATE INDEX IF NOT EXISTS idx_timesheet_org_crew ON timesheets(org_id, crew_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_org_archived ON timesheets(org_id, archived_at);

-- Create timesheet_entries table
CREATE TABLE IF NOT EXISTS timesheet_entries (
    id BIGSERIAL PRIMARY KEY,
    timesheet_id BIGINT NOT NULL,
    task_id VARCHAR(255),
    hours DOUBLE PRECISION NOT NULL,
    hours_type VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timesheet_entries_timesheet FOREIGN KEY (timesheet_id) REFERENCES timesheets(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_timesheet_entries_timesheet ON timesheet_entries(timesheet_id);
CREATE INDEX IF NOT EXISTS idx_timesheet_entries_task ON timesheet_entries(task_id);

-- Create crew_assignments table
CREATE TABLE IF NOT EXISTS crew_assignments (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    crew_id VARCHAR(255) NOT NULL,
    role_key VARCHAR(255),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255),
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crew_assignments_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_assignments_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_assignments_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_crew_assign_org_project_person ON crew_assignments(org_id, project_id, person_id);
CREATE INDEX IF NOT EXISTS idx_crew_assign_org_crew ON crew_assignments(org_id, crew_id);
CREATE INDEX IF NOT EXISTS idx_crew_assign_org_status ON crew_assignments(org_id, status);
CREATE INDEX IF NOT EXISTS idx_crew_assign_org_dates ON crew_assignments(org_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_crew_assign_org_archived ON crew_assignments(org_id, archived_at);

-- Create crew_swaps table
CREATE TABLE IF NOT EXISTS crew_swaps (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    from_crew_id VARCHAR(255) NOT NULL,
    to_crew_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_by VARCHAR(255),
    requested_at TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    rejected_by VARCHAR(255),
    rejected_at TIMESTAMP,
    rejection_reason TEXT,
    completed_by VARCHAR(255),
    completed_at TIMESTAMP,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crew_swaps_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_swaps_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_crew_swaps_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_crew_swap_org_project_person ON crew_swaps(org_id, project_id, person_id);
CREATE INDEX IF NOT EXISTS idx_crew_swap_org_status ON crew_swaps(org_id, status);
CREATE INDEX IF NOT EXISTS idx_crew_swap_org_created ON crew_swaps(org_id, created_at);
CREATE INDEX IF NOT EXISTS idx_crew_swap_org_archived ON crew_swaps(org_id, archived_at);

COMMENT ON TABLE timesheets IS 'Timesheet records for project/person/date';
COMMENT ON TABLE timesheet_entries IS 'Timesheet line entries';
COMMENT ON TABLE crew_assignments IS 'Crew assignment history';
COMMENT ON TABLE crew_swaps IS 'Crew swap requests and approvals';
