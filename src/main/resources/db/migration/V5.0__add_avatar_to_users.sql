-- ══════════════════════════════════════════════════════════════════════
-- V5.0 — Add avatar column to users table
-- ══════════════════════════════════════════════════════════════════════
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(500) NULL;

COMMENT ON COLUMN users.avatar IS 'S3 key or full URL of the user avatar image';
