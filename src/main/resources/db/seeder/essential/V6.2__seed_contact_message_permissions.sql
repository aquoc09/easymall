-- V6.2: Seed Contact Message Permissions
INSERT INTO permissions (permission_name, description) VALUES
('contact:read', 'Read contact messages'),
('contact:update', 'Update contact messages status')
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
        'contact:read',
        'contact:update'
    ) ON CONFLICT (role_id, permission_id) DO NOTHING;
