-- V4.8__add_address_ghn_fields.sql
-- 1. Thêm tên tỉnh/huyện/xã vào bảng addresses (lưu lúc tạo từ GHN master data)
-- 2. Seed permissions cho Address management + GHN read

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 1: Thêm display name columns vào addresses
-- ──────────────────────────────────────────────────────────────────────
ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS province_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS district_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ward_name     VARCHAR(100);

