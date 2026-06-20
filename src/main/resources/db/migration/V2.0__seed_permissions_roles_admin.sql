-- ══════════════════════════════════════════════════════════════════════
-- V2.0: Seed permissions, admin account, and role-permission assignments
-- ══════════════════════════════════════════════════════════════════════

-- Enable pgcrypto for BCrypt password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ── 1. Seed Permissions ─────────────────────────────────────────────
INSERT INTO permissions (permission_name, description) VALUES
    ('user:read',       'View user list and details'),
    ('user:create',     'Create new user'),
    ('user:update',     'Update user information'),
    ('user:delete',     'Deactivate user'),
    ('profile:read',    'View own profile'),
    ('profile:update',  'Update own profile'),
    ('role:read',       'View roles'),
    ('role:create',     'Create new role'),
    ('role:update',     'Update role and assign permissions'),
    ('role:delete',     'Delete role'),
    ('permission:read',   'View permissions'),
    ('permission:create', 'Create new permission'),
    ('permission:update', 'Update permission'),
    ('permission:delete', 'Delete permission'),
    ('category:read',   'View categories (admin)'),
    ('category:create', 'Create category'),
    ('category:update', 'Update category'),
    ('category:delete', 'Delete category'),
    ('address:read',    'View addresses'),
    ('address:create',  'Create address'),
    ('address:update',  'Update address'),
    ('address:delete',  'Delete address')
ON CONFLICT (permission_name) DO NOTHING;

-- ── 2. Seed Admin Account ───────────────────────────────────────────
-- Password: admin@123 (BCrypt encoded via pgcrypto)
INSERT INTO users (email, password, full_name, is_active, role_id)
SELECT 'admin@easymall.com',
       crypt('admin@123', gen_salt('bf', 10)),
       'System Admin',
       true,
       r.role_id
FROM roles r
WHERE r.role_name = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'admin@easymall.com');

-- ── 3. Assign ALL permissions to ROLE_ADMIN ─────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── 4. Assign limited permissions to ROLE_USER ──────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_USER'
  AND p.permission_name IN (
      'profile:read',
      'profile:update',
      'address:read',
      'address:create',
      'address:update',
      'address:delete'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
