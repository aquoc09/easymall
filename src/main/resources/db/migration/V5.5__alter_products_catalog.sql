-- ══════════════════════════════════════════════════════════════════════
-- V5.5 — Alter products table (Catalog refactor)
-- 1. Fix created_at: TIMESTAMP → TIMESTAMPTZ
-- 2. Add updated_at + trigger
-- 3. Add denormalized stats columns
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: Fix timezone-aware timestamp
ALTER TABLE products
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Ho_Chi_Minh';

-- Step 2: Add updated_at
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

UPDATE products SET updated_at = created_at WHERE updated_at IS NULL;

CREATE TRIGGER trg_products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Step 3: Denormalized stats — managed by DB trigger (min/max) and application/cron (others)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS min_price        NUMERIC(15,2)  DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS max_price        NUMERIC(15,2)  DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS view_count       INT            DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sold_count       INT            DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating_avg       DECIMAL(3,2)   DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS rating_count     INT            DEFAULT 0,
    ADD COLUMN IF NOT EXISTS popularity_score DECIMAL(10,4)  DEFAULT 0.00;
