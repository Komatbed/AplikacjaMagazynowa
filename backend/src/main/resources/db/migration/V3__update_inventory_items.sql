-- V3__update_inventory_items.sql
-- Add missing columns to inventory_items table to match InventoryItem entity

ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS internal_color VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS external_color VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS core_color VARCHAR(50);
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS reserved_by VARCHAR(100);
ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS reservation_date TIMESTAMP;
