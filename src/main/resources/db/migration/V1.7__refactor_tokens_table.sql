-- Step 1: Drop the old refresh_token column (VARCHAR 255 — too short for a JWT, semantically confusing)
ALTER TABLE tokens DROP COLUMN refresh_token;

-- Step 2: Rename token_value → refresh_token (this column IS the RT JWT string, needs 1000 chars)
ALTER TABLE tokens RENAME COLUMN token_value TO refresh_token;

-- Step 3: Add RT expiry (checked before rotation) and device tracking (audit)
ALTER TABLE tokens
    ADD COLUMN expires_at  TIMESTAMPTZ,
    ADD COLUMN device_info VARCHAR(255);
