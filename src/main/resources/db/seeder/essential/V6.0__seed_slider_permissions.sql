-- V6.0: Seed Slider Permissions
INSERT INTO permissions (permission_name, description) VALUES
('slider:read', 'Read slider'),
('slider:create', 'Create slider'),
('slider:update', 'Update slider'),
('slider:delete', 'Delete slider')
ON CONFLICT (permission_name) DO NOTHING;

-- Assign these permissions to the Admin role
INSERT INTO
    role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
    CROSS JOIN permissions p
WHERE
    r.role_name = 'ROLE_ADMIN'
    AND p.permission_name IN (
        'slider:read',
        'slider:create',
        'slider:update',
        'slider:delete'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;
