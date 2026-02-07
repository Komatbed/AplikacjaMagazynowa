-- V2__add_missing_tables.sql
-- Add users, messages, shortages tables to match Web UI requirements

-- 1. Users Table (Authentication)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY, -- Matches User entity UUID string
    login VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Initial Admin User will be created by DataInitializer to ensure correct password hashing


-- 2. Messages Table (Communication Module)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_recipient ON messages(recipient_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);


-- 3. Shortages Table (Supply Module)
CREATE TABLE IF NOT EXISTS shortages (
    id UUID PRIMARY KEY,
    reported_by_id UUID REFERENCES users(id),
    item_name VARCHAR(100) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, CRITICAL
    status VARCHAR(20) DEFAULT 'NEW',      -- NEW, IN_PROGRESS, RESOLVED, REJECTED
    reported_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shortages_status ON shortages(status);


-- 4. Claims / Issues (Aligning with IssueReport)
-- Ensure IssueReport table exists (from V1) and add missing columns if any
-- V1 created 'issue_reports' (implicit from JPA default naming if not specified in V1)
-- Let's check V1 content again. V1 did NOT create 'issue_reports' table explicitly!
-- V1 only created: producers, profile_systems, profile_types, colors, locations, location_assignments, inventory_items, operation_logs.
-- So we MUST create issue_reports here.

CREATE TABLE IF NOT EXISTS issue_reports (
    id UUID PRIMARY KEY,
    reported_by_id UUID REFERENCES users(id),
    order_number VARCHAR(50),
    delivery_date DATE,
    description TEXT,
    part_number VARCHAR(50),
    quantity INT,
    status VARCHAR(20) DEFAULT 'NEW', -- NEW, IN_PROGRESS, RESOLVED, REJECTED
    decision_note TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Add photo support for issues
CREATE TABLE IF NOT EXISTS issue_photos (
    id UUID PRIMARY KEY,
    issue_report_id UUID REFERENCES issue_reports(id),
    photo_url VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT NOW()
);
