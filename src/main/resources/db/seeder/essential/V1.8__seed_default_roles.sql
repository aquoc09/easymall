INSERT INTO roles (role_name) VALUES ('ROLE_USER')  ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('ROLE_ADMIN') ON CONFLICT (role_name) DO NOTHING;
