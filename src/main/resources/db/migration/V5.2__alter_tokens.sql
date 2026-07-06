-- ══════════════════════════════════════════════════════════════════════
-- V5.2 — Alter tokens table
-- 1. refresh_token: VARCHAR(1000) → VARCHAR(300)
-- 2. expires_at: nullable → NOT NULL
-- 3. Drop device_info
-- 4. Add performance indexes
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: Resize refresh_token (JWT RT thực tế ~200-280 chars, dùng 300 làm buffer)
ALTER TABLE tokens ALTER COLUMN refresh_token TYPE VARCHAR(300);

-- Step 2: expires_at NOT NULL
--   Gán giá trị cho các row NULL (nếu có) trước khi set NOT NULL
UPDATE tokens SET expires_at = CURRENT_TIMESTAMP WHERE expires_at IS NULL;
ALTER TABLE tokens ALTER COLUMN expires_at SET NOT NULL;
ALTER TABLE tokens ALTER COLUMN expires_at SET DEFAULT CURRENT_TIMESTAMP;

-- Step 3: Xoá device_info (tracking device đã chuyển sang device_sessions)
ALTER TABLE tokens DROP COLUMN IF EXISTS device_info;

-- Step 4: Indexes tăng tốc lookup
CREATE INDEX IF NOT EXISTS idx_tokens_user    ON tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_refresh ON tokens (refresh_token);
