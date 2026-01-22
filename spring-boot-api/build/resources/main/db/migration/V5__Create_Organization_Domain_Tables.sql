-- V5: Create Organization Domain Tables
-- Tables: offices, org_taxonomies, graph_edges
-- Order: Create offices first (depends on organizations, self-referencing), 
--        then org_taxonomies and graph_edges (depend on organizations)

-- Create offices table
CREATE TABLE IF NOT EXISTS offices (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    org_id BIGINT NOT NULL,
    description TEXT,
    timezone VARCHAR(255),
    org_location_type_key VARCHAR(255),
    parent_org_location_id BIGINT,
    sort_order INTEGER,
    address TEXT,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_office_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_office_parent FOREIGN KEY (parent_org_location_id) REFERENCES offices(id) ON DELETE SET NULL
);

-- Create office_tag_keys element collection table
CREATE TABLE IF NOT EXISTS office_tag_keys (
    office_id BIGINT NOT NULL,
    tag_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (office_id, tag_key),
    CONSTRAINT fk_office_tag_keys_office FOREIGN KEY (office_id) REFERENCES offices(id) ON DELETE CASCADE
);

-- Create indexes for offices
CREATE UNIQUE INDEX IF NOT EXISTS idx_office_org_normalized_name 
    ON offices(org_id, normalized_name) 
    WHERE archived_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_office_org_archived ON offices(org_id, archived_at);
CREATE INDEX IF NOT EXISTS idx_office_org_parent ON offices(org_id, parent_org_location_id);

COMMENT ON TABLE offices IS 'Organization locations/offices';
COMMENT ON TABLE office_tag_keys IS 'Office tag keys element collection';

-- Create org_taxonomies table
CREATE TABLE IF NOT EXISTS org_taxonomies (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_taxonomy_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT uk_org_taxonomy_org_namespace UNIQUE (org_id, namespace)
);

-- Create org_taxonomy_values element collection table
CREATE TABLE IF NOT EXISTS org_taxonomy_values (
    org_taxonomy_id BIGINT NOT NULL,
    key VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    sort_order INTEGER,
    color VARCHAR(255),
    metadata JSONB,
    archived_at TIMESTAMP,
    PRIMARY KEY (org_taxonomy_id, key),
    CONSTRAINT fk_org_taxonomy_values_taxonomy FOREIGN KEY (org_taxonomy_id) REFERENCES org_taxonomies(id) ON DELETE CASCADE
);

COMMENT ON TABLE org_taxonomies IS 'Organization-specific taxonomy values';
COMMENT ON TABLE org_taxonomy_values IS 'Org taxonomy values element collection';

-- Create graph_edges table
CREATE TABLE IF NOT EXISTS graph_edges (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    from_node_type VARCHAR(50) NOT NULL CHECK (from_node_type IN ('person', 'org_location', 'company', 'company_location')),
    from_node_id BIGINT NOT NULL,
    to_node_type VARCHAR(50) NOT NULL CHECK (to_node_type IN ('person', 'org_location', 'company', 'company_location')),
    to_node_id BIGINT NOT NULL,
    edge_type_key VARCHAR(255) NOT NULL,
    metadata JSONB DEFAULT '{}',
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    archived_at TIMESTAMP,
    pii_stripped BOOLEAN NOT NULL DEFAULT FALSE,
    legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_graph_edge_org FOREIGN KEY (org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- Create indexes for graph_edges
CREATE UNIQUE INDEX IF NOT EXISTS idx_graph_edge_unique 
    ON graph_edges(org_id, edge_type_key, from_node_type, from_node_id, to_node_type, to_node_id) 
    WHERE archived_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_graph_edge_org_from ON graph_edges(org_id, from_node_type, from_node_id);
CREATE INDEX IF NOT EXISTS idx_graph_edge_org_to ON graph_edges(org_id, to_node_type, to_node_id);
CREATE INDEX IF NOT EXISTS idx_graph_edge_org_archived ON graph_edges(org_id, archived_at);

COMMENT ON TABLE graph_edges IS 'Generic graph relationships between entities';

