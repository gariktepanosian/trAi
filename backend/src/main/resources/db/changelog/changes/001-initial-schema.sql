--liquibase formatted sql

--changeset trai:001-initial-schema
CREATE TABLE IF NOT EXISTS trai_users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_trail_events (
    id VARCHAR(64) PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    input_snippet TEXT,
    verdict VARCHAR(100),
    trust_score INT,
    flags TEXT,
    blocked BOOLEAN DEFAULT FALSE,
    latency_ms BIGINT,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS b2b_partners (
    id VARCHAR(64) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL UNIQUE,
    signing_secret VARCHAR(255) NOT NULL,
    callback_url VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
