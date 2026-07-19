-- ══════════════════════════════════════════════════════════════════════
-- V2.6: Seed product permissions and assign to ROLE_ADMIN
-- ══════════════════════════════════════════════════════════════════════

-- ── 1. Seed Product Permissions ─────────────────────────────────────
INSERT INTO permissions (permission_name, description) VALUES
    ('product:read',   'View products (public)'),
    ('product:create', 'Create new product'),
    ('product:update', 'Update product information'),
    ('product:delete', 'Delete product')
ON CONFLICT (permission_name) DO NOTHING;

-- ── 2. Assign all new permissions to ROLE_ADMIN ──────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_name IN ('product:read', 'product:create', 'product:update', 'product:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;
