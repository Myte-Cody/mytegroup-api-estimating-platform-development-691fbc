-- Initial schema for Spring Boot migration
-- This migration sets up the basic structure and audit tables for Hibernate Envers

-- Create schema if it doesn't exist (PostgreSQL 18)
CREATE SCHEMA IF NOT EXISTS public;

-- Hibernate Envers requires a revision table
-- This will be created automatically by Hibernate Envers, but we can create it manually for better control
CREATE TABLE IF NOT EXISTS revinfo (
    rev INTEGER NOT NULL PRIMARY KEY,
    revtstmp BIGINT
);

-- Create a sequence for revision numbers if it doesn't exist
CREATE SEQUENCE IF NOT EXISTS revinfo_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Set the sequence owner
ALTER SEQUENCE revinfo_seq OWNED BY revinfo.rev;

-- Example: This is a placeholder for future entity tables
-- Actual entity tables will be created via JPA/Hibernate DDL or additional Flyway migrations
-- Example structure (commented out):
-- CREATE TABLE IF NOT EXISTS example_entity (
--     id BIGSERIAL PRIMARY KEY,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );

-- Create indexes for common audit fields (will be used by Envers audit tables)
-- These will be created automatically by Envers, but can be customized here if needed

COMMENT ON TABLE revinfo IS 'Hibernate Envers revision information table';
COMMENT ON SEQUENCE revinfo_seq IS 'Sequence for Hibernate Envers revision numbers';

