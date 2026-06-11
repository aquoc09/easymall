CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT REFERENCES roles (role_id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions (permission_id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id) -- Một role chỉ có 1 quyền cụ thể 1 lần
);