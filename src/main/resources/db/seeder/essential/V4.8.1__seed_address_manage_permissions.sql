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
