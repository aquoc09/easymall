-- 2. Seed permissions cho Cart
INSERT INTO
    permissions (permission_name, description)
VALUES ('cart:view', 'View own cart'),
    (
        'cart:manage',
        'Add, update, remove items in own cart'
    ) ON CONFLICT (permission_name) DO NOTHING;

-- 3. Gán permissions cho role USER
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'USER'
    AND p.permission_name IN ('cart:view', 'cart:manage') ON CONFLICT (role_id, permission_id) DO NOTHING;
