-- ══════════════════════════════════════════════════════════════════════
-- V3.4 — Seed permissions: Coupon & Order
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO
    permissions (permission_name, description)
VALUES (
        'coupon:manage',
        'Admin CRUD coupons on platform'
    ),
    (
        'coupon:apply',
        'User preview/apply coupon discount'
    ),
    (
        'order:create',
        'User place order (checkout)'
    ),
    (
        'order:view',
        'User view own orders'
    ),
    (
        'order:manage',
        'User manage own orders (cancel)'
    ),
    (
        'order:admin',
        'Admin view and update all orders'
    ) ON CONFLICT (permission_name) DO NOTHING;

-- ── Gán cho USER ────────────────────────────────────────────────────
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'USER'
    AND p.permission_name IN (
        'coupon:apply',
        'order:create',
        'order:view',
        'order:manage'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ── Gán cho ADMIN (bao gồm coupon:manage — Admin only) ──────────────
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'ADMIN'
    AND p.permission_name IN (
        'coupon:manage',
        'coupon:apply',
        'order:admin',
        'order:view',
        'order:create',
        'order:manage'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;