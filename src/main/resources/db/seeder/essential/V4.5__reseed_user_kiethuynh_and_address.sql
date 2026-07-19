-- V4.5__reseed_user_kiethuynh_and_address.sql
-- Reseed user kiethuynh3499@gmail.com with password and address

-- 1. Xóa dữ liệu cũ nếu có để tránh conflict
DELETE FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'kiethuynh3499@gmail.com');
DELETE FROM users WHERE email = 'kiethuynh3499@gmail.com';

-- 2. Insert User (Mã hóa password dùng bcrypt)
INSERT INTO users (
    email,
    password,
    full_name,
    gender,
    phone,
    is_active,
    role_id
)
VALUES (
    'kiethuynh3499@gmail.com',
    crypt('Password@123', gen_salt('bf', 10)), -- Password mặc định: Password@123
    'Kiet Huynh',
    1,
    '0909123456',
    TRUE,
    (SELECT role_id FROM roles WHERE role_name = 'ROLE_USER')
);

-- 3. Insert Default Address
INSERT INTO addresses (
    recipient_name,
    phone,
    province_id,
    district_id,
    ward_code,
    full_address,
    street_number,
    is_default,
    user_id
)
VALUES (
    'Kiet Huynh',
    '0909123456',
    202,
    1442,
    '20209',
    'So 1 Nguyen Hue, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh',
    'So 1 Nguyen Hue',
    TRUE,
    (SELECT user_id FROM users WHERE email = 'kiethuynh3499@gmail.com')
);
