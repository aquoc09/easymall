-- ══════════════════════════════════════════════════════════════════════
-- V3.5__seed_demo_users_and_addresses.sql
-- Seed demo users + addresses dùng cho các seeder tiếp theo (coupon, order)
-- Phụ thuộc: V1.8 (roles đã có ROLE_USER / ROLE_ADMIN)
-- Password hash là BCrypt của "Demo@1234"
-- ══════════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 1: DEMO USERS
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO users (
    email,
    full_name,
    gender,
    phone,
    dob,
    is_active,
    role_id
)
VALUES
    -- ─── User 1: Admin ────────────────────────────────────────────────
    (
        'admin@easymall.vn',
        'EasyMall Admin',
        1,            -- Nam
        '0901000001',
        '1990-01-01',
        TRUE,
        (SELECT role_id FROM roles WHERE role_name = 'ROLE_ADMIN')
    ),

    -- ─── User 2: Demo buyer (nguoi mua hang demo) ─────────────────────
    (
        'buyer@easymall.vn',
        'Nguyen Van Demo',
        1,            -- Nam
        '0901000002',
        '1995-06-15',
        TRUE,
        (SELECT role_id FROM roles WHERE role_name = 'ROLE_USER')
    ),

    -- ─── User 3: Demo buyer 2 ─────────────────────────────────────────
    (
        'buyer2@easymall.vn',
        'Tran Thi Demo',
        0,            -- Nu
        '0901000003',
        '1998-03-20',
        TRUE,
        (SELECT role_id FROM roles WHERE role_name = 'ROLE_USER')
    )

ON CONFLICT (email) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 2: DEMO ADDRESSES
-- Dùng GHN province/district/ward code mẫu (Ho Chi Minh City)
--   province_id = 202 (TP. Ho Chi Minh)
--   district_id = 1442 (Quan 1)
--   ward_code   = '20209' (Phuong Ben Nghe)
-- ──────────────────────────────────────────────────────────────────────

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
VALUES
    -- ─── Address 1: Dia chi mac dinh cua buyer 1 ──────────────────────
    (
        'Nguyen Van Demo',
        '0901000002',
        202,
        1442,
        '20209',
        'So 1 Nguyen Hue, Phuong Ben Nghe, Quan 1, TP. Ho Chi Minh',
        'So 1 Nguyen Hue',
        TRUE,
        (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn')
    ),

    -- ─── Address 2: Dia chi thu 2 cua buyer 1 ─────────────────────────
    (
        'Nguyen Van Demo - VP',
        '0901000002',
        202,
        1442,
        '20209',
        'So 10 Le Loi, Phuong Ben Thanh, Quan 1, TP. Ho Chi Minh',
        'So 10 Le Loi',
        FALSE,
        (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn')
    ),

    -- ─── Address 3: Dia chi mac dinh cua buyer 2 ──────────────────────
    (
        'Tran Thi Demo',
        '0901000003',
        202,
        1442,
        '20209',
        'So 5 Pham Ngu Lao, Phuong Pham Ngu Lao, Quan 1, TP. Ho Chi Minh',
        'So 5 Pham Ngu Lao',
        TRUE,
        (SELECT user_id FROM users WHERE email = 'buyer2@easymall.vn')
    )

ON CONFLICT DO NOTHING;
