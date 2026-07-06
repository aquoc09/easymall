-- ══════════════════════════════════════════════════════════════════════
-- V5.3 — Alter addresses table
-- Drop province_name, district_name, ward_name
-- (GHN display names sẽ được resolve tại tầng service/response, không lưu DB)
-- ══════════════════════════════════════════════════════════════════════

ALTER TABLE addresses
    DROP COLUMN IF EXISTS province_name,
    DROP COLUMN IF EXISTS district_name,
    DROP COLUMN IF EXISTS ward_name;
