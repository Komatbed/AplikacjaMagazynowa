-- V4__add_config_definitions.sql
-- Add tables for ProfileDefinition and ColorDefinition entities

CREATE TABLE IF NOT EXISTS profile_definitions (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    height_mm INT DEFAULT 0,
    width_mm INT DEFAULT 0,
    bead_height_mm INT DEFAULT 0,
    bead_angle DOUBLE PRECISION DEFAULT 0.0,
    standard_length_mm INT DEFAULT 6500,
    system VARCHAR(100),
    manufacturer VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS color_definitions (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    name VARCHAR(100),
    palette_code VARCHAR(50),
    veka_code VARCHAR(50),
    type VARCHAR(50) DEFAULT 'smooth',
    foil_manufacturer VARCHAR(100)
);
