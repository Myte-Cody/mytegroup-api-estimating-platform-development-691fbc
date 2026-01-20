-- V9: Create Communication Domain Tables
-- Tables: email_templates, contact_inquiries, notifications
-- Order: All depend on organizations, create in any order

-- Create email_templates table
CREATE TABLE IF NOT EXISTS email_templates (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    locale VARCHAR(50) NOT NULL DEFAULT 'en',
    subject VARCHAR(255) NOT NULL,
    html TEXT NOT NULL,
    text TEXT NOT NULL,
    created_by_user_id BIGINT,
    updated_by_user_id BIGINT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_template_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_email_template_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_email_template_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_email_template_org_name_locale UNIQUE (org_id, name, locale)
);

-- Create email_template_required_variables element collection table
CREATE TABLE IF NOT EXISTS email_template_required_variables (
    email_template_id BIGINT NOT NULL,
    variable VARCHAR(255) NOT NULL,
    PRIMARY KEY (email_template_id, variable),
    CONSTRAINT fk_email_template_required_variables_template FOREIGN KEY (email_template_id) REFERENCES email_templates(id) ON DELETE CASCADE
);

-- Create email_template_optional_variables element collection table
CREATE TABLE IF NOT EXISTS email_template_optional_variables (
    email_template_id BIGINT NOT NULL,
    variable VARCHAR(255) NOT NULL,
    PRIMARY KEY (email_template_id, variable),
    CONSTRAINT fk_email_template_optional_variables_template FOREIGN KEY (email_template_id) REFERENCES email_templates(id) ON DELETE CASCADE
);

COMMENT ON TABLE email_templates IS 'Email templates';
COMMENT ON TABLE email_template_required_variables IS 'Email template required variables element collection';
COMMENT ON TABLE email_template_optional_variables IS 'Email template optional variables element collection';

-- Create contact_inquiries table
CREATE TABLE IF NOT EXISTS contact_inquiries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    source VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'new' CHECK (status IN ('new', 'in-progress', 'closed')),
    ip VARCHAR(45),
    user_agent VARCHAR(512),
    responded_at TIMESTAMP,
    responded_by VARCHAR(255),
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for contact_inquiries
CREATE INDEX IF NOT EXISTS idx_contact_inquiry_created_at ON contact_inquiries(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contact_inquiry_status_created ON contact_inquiries(status, created_at DESC);

COMMENT ON TABLE contact_inquiries IS 'Contact form inquiries';

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB DEFAULT '{}',
    read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notification_org_user_read_created ON notifications(org_id, user_id, read, created_at DESC);

COMMENT ON TABLE notifications IS 'User notifications';

