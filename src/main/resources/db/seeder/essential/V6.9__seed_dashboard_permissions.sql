-- Seed permission for Dashboard
INSERT INTO permissions (permission_name)
VALUES
    ('dashboard:view')
ON CONFLICT (permission_name) DO NOTHING;

-- Grant permissions to ROLE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_name IN (
      'dashboard:view'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
