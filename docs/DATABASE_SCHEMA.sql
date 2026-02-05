-- Database Schema for Warehouse Production System
-- PostgreSQL

-- 1. Tabele Słownikowe (Ukryte przed pracownikiem hali, używane przez system/kierownika)
CREATE TABLE producers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code_prefix VARCHAR(10) -- np. dla rozpoznawania OCR
);

CREATE TABLE profile_systems (
    id SERIAL PRIMARY KEY,
    producer_id INT REFERENCES producers(id),
    name VARCHAR(100) NOT NULL
);

CREATE TABLE profile_types (
    id SERIAL PRIMARY KEY,
    system_id INT REFERENCES profile_systems(id),
    code VARCHAR(50) NOT NULL, -- np. "P-1234" (to widzi pracownik jako "Numer Profila")
    description TEXT,
    cross_section_image_url VARCHAR(255)
);

CREATE TABLE colors (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL, -- np. "A01"
    name VARCHAR(100) NOT NULL, -- np. "Złoty Dąb"
    is_core BOOLEAN DEFAULT FALSE -- czy to kolor rdzenia
);

-- 2. Konfiguracja Magazynowa (Mapping fizyczny)
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    row_number INT NOT NULL, -- Rząd 1-25
    palette_number INT NOT NULL, -- 1-3 w rzędzie
    label VARCHAR(10) NOT NULL UNIQUE, -- np. "12B" (Rząd 12, Paleta B)
    is_waste_palette BOOLEAN DEFAULT FALSE -- Czy to paleta na odpady/ścinki
);

-- Przypisanie co ma leżeć na danej lokalizacji (Slotting)
CREATE TABLE location_assignments (
    id SERIAL PRIMARY KEY,
    location_id INT REFERENCES locations(id),
    profile_type_id INT REFERENCES profile_types(id),
    color_inner_id INT REFERENCES colors(id), -- Kolor wew
    color_outer_id INT REFERENCES colors(id), -- Kolor zew
    length_mm INT NOT NULL, -- Długość standardowa (dla całych sztang) lub 0 dla odpadów
    min_quantity_threshold INT DEFAULT 10, -- Alarm poniżej tego stanu
    UNIQUE(location_id) -- Jedna lokalizacja = jedna konfiguracja (uproszczenie dla hali)
);

-- 3. Stan Magazynowy (Inventory)
-- Uwaga: Dla całych sztang (Full Bars) i Odpadów (Waste)
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id INT REFERENCES locations(id),
    
    -- Denormalizacja dla szybkości odczytu na Androidzie (opcjonalne, ale pomocne)
    profile_code VARCHAR(50) NOT NULL, 
    
    length_mm INT NOT NULL, -- Faktyczna długość (dla odpadu może być inna niż standard)
    quantity INT NOT NULL DEFAULT 0,
    
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- AVAILABLE, RESERVED, DAMAGED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 4. Operacje i Audyt (Logowanie wszystkiego)
CREATE TABLE operation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(50) NOT NULL, -- TAKEN, ADDED, MOVED, WASTE_CREATED, LABEL_PRINTED
    
    user_id UUID, -- ID pracownika
    device_id VARCHAR(50), -- ID tabletu
    
    inventory_item_id UUID REFERENCES inventory_items(id),
    location_id INT REFERENCES locations(id),
    
    quantity_change INT NOT NULL, -- np. -1, +5
    
    reason VARCHAR(100), -- np. "Produkcja", "Zniszczenie", "Korekta"
    photo_url VARCHAR(255), -- Dla reklamacji/zniszczeń
    
    timestamp TIMESTAMP DEFAULT NOW(),
    synced_at TIMESTAMP -- Kiedy trafiło do głównej bazy (dla offline)
);

-- 5. Indeksy
CREATE INDEX idx_inventory_location ON inventory_items(location_id);
CREATE INDEX idx_logs_timestamp ON operation_logs(timestamp);
CREATE INDEX idx_profile_code ON profile_types(code);

-- 6. Przykładowe dane
-- Insert producer
INSERT INTO producers (name, code_prefix) VALUES ('Aluplast', 'ALU');
-- Insert system
INSERT INTO profile_systems (producer_id, name) VALUES (1, 'Ideal 4000');
-- Insert type
INSERT INTO profile_types (system_id, code, description) VALUES (1, '140001', 'Rama okienna 70mm');
-- Insert colors
INSERT INTO colors (code, name) VALUES ('W', 'Biały'), ('ZD', 'Złoty Dąb');
-- Insert location
INSERT INTO locations (row_number, palette_number, label) VALUES (1, 1, '01A');
-- Assign product to location
INSERT INTO location_assignments (location_id, profile_type_id, color_inner_id, color_outer_id, length_mm)
VALUES (1, 1, 1, 1, 6500); -- Biały/Biały, 6.5m
