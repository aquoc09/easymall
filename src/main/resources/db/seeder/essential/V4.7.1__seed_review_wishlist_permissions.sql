-- SECTION 3: Seed permissions
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO permissions (permission_name)
VALUES
    ('review:create'),
    ('review:view'),
    ('review:moderate'),
    ('review:delete'),
    ('wishlist:view'),
    ('wishlist:manage')
ON CONFLICT (permission_name) DO NOTHING;

-- ROLE_USER: tạo, xem, xóa review của chính mình + quản lý wishlist
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_USER'
  AND p.permission_name IN (
      'review:create',
      'review:view',
      'review:delete',
      'wishlist:view',
      'wishlist:manage'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_ADMIN: tất cả permissions trên + review:moderate
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_name IN (
      'review:create',
      'review:view',
      'review:moderate',
      'review:delete',
      'wishlist:view',
      'wishlist:manage'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
