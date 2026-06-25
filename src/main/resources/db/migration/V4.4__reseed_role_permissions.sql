-- V4.4: Reseed permissions for Cart, Coupon, and Order because V2.9 and V3.5 used incorrect role names

-- Seed permissions cho Cart module
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'ROLE_USER'
    AND p.permission_name IN ('cart:view', 'cart:manage') ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Seed permissions cho Coupon & Order modules
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'ROLE_USER'
    AND p.permission_name IN (
        'coupon:apply',
        'order:create',
        'order:view',
        'order:manage'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'ROLE_ADMIN'
    AND p.permission_name IN (
        'coupon:manage',
        'coupon:apply',
        'order:admin',
        'order:view',
        'order:create',
        'order:manage'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;
