-- =====================================================================
-- V2.9: Fix stock_quantity constraint để hỗ trợ -1 (vô cực/bán order)
--       và seed permissions cho Cart module
-- =====================================================================

-- 1. Xóa constraint cũ (chỉ cho phép >= 0) và tạo constraint mới
--    Cho phép: -1 (vô cực) hoặc >= 0 (số thực)
ALTER TABLE product_variants
    DROP CONSTRAINT IF EXISTS chk_stock_valid;

ALTER TABLE product_variants
    ADD CONSTRAINT chk_stock_valid CHECK (stock_quantity = -1 OR stock_quantity >= 0);

-- 2. Seed permissions cho Cart
INSERT INTO permissions (permission_name, description) VALUES
    ('cart:view',   'View own cart'),
    ('cart:manage', 'Add, update, remove items in own cart')
ON CONFLICT (permission_name) DO NOTHING;

-- 3. Gán permissions cho role USER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'USER'
  AND p.permission_name IN ('cart:view', 'cart:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

