-- V1__init_schema.sql
-- Database Schema for Warehouse Production System

-- 1. Tabele Słownikowe
CREATE TABLE IF NOT EXISTS producers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code_prefix VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS profile_systems (
    id SERIAL PRIMARY KEY,
    producer_id INT REFERENCES producers(id),
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS profile_types (
    id SERIAL PRIMARY KEY,
    system_id INT REFERENCES profile_systems(id),
    code VARCHAR(50) NOT NULL,
    description TEXT,
    cross_section_image_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS colors (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_core BOOLEAN DEFAULT FALSE
);

-- 2. Konfiguracja Magazynowa
CREATE TABLE IF NOT EXISTS locations (
    id SERIAL PRIMARY KEY,
    row_number INT NOT NULL,
    palette_number INT NOT NULL,
    label VARCHAR(10) NOT NULL UNIQUE,
    is_waste_palette BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS location_assignments (
    id SERIAL PRIMARY KEY,
    location_id INT REFERENCES locations(id) UNIQUE,
    profile_type_id INT REFERENCES profile_types(id),
    color_inner_id INT REFERENCES colors(id),
    color_outer_id INT REFERENCES colors(id),
    length_mm INT NOT NULL,
    min_quantity_threshold INT DEFAULT 10
);

-- 3. Stan Magazynowy
CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY, -- Flyway uses standard SQL, ensure extension is enabled if using gen_random_uuid() or handle in app
    location_id INT REFERENCES locations(id),
    profile_code VARCHAR(50) NOT NULL,
    length_mm INT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 4. Operacje i Audyt
CREATE TABLE IF NOT EXISTS operation_logs (
    id UUID PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,
    user_id UUID,
    device_id VARCHAR(50),
    inventory_item_id UUID REFERENCES inventory_items(id),
    location_id INT REFERENCES locations(id),
    quantity_change INT NOT NULL,
    reason VARCHAR(100),
    photo_url VARCHAR(255),
    timestamp TIMESTAMP DEFAULT NOW(),
    synced_at TIMESTAMP
);

-- 5. Indeksy
CREATE INDEX IF NOT EXISTS idx_inventory_location ON inventory_items(location_id);
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON operation_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_profile_code ON profile_types(code);
