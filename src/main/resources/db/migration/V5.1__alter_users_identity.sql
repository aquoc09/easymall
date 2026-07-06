-- ══════════════════════════════════════════════════════════════════════
-- V5.1 — Alter users table (Identity refactor)
-- 1. password: NOT NULL → NULL (hỗ trợ OAuth-only account)
-- 2. Add email_verified_at, last_login_at
-- 3. Add CHECK constraint chk_users_password
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: Cho phép password NULL (OAuth user không cần password)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password DROP DEFAULT;

-- Step 2: Thêm audit / verification fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS last_login_at     TIMESTAMPTZ NULL;

-- Step 3: Đảm bảo user luôn có ít nhất 1 phương thức xác thực
ALTER TABLE users
    ADD CONSTRAINT chk_users_password
    CHECK (
        password IS NOT NULL
        OR google_account_id   IS NOT NULL
        OR facebook_account_id IS NOT NULL
    );
