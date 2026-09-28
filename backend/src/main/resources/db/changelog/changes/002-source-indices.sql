--liquibase formatted sql

--changeset trai:002-indices
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_trail_events(actor);
CREATE INDEX IF NOT EXISTS idx_audit_action_type ON audit_trail_events(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_recorded_at ON audit_trail_events(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_b2b_email ON b2b_partners(contact_email);
