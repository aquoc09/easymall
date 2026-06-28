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

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 2: Seed permissions
-- ──────────────────────────────────────────────────────────────────────
INSERT INTO permissions (permission_name)
VALUES
    ('address:manage'),
    ('ghn:read')
ON CONFLICT (permission_name) DO NOTHING;

-- ROLE_USER: quản lý address của chính mình + đọc master data GHN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_USER'
  AND p.permission_name IN ('address:manage', 'ghn:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_ADMIN: cũng có các permission trên
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_name IN ('address:manage', 'ghn:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;
