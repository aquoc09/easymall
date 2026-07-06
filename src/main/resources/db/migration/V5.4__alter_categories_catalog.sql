-- ══════════════════════════════════════════════════════════════════════
-- V5.4 — Alter categories table (Catalog refactor)
-- 1. Drop target_demographic, category_type (đơn giản hoá, dùng target_gender trên products)
-- 2. Add updated_at + trigger
-- ══════════════════════════════════════════════════════════════════════

ALTER TABLE categories
    DROP COLUMN IF EXISTS target_demographic,
    DROP COLUMN IF EXISTS category_type;

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

-- Backfill
UPDATE categories SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

CREATE TRIGGER trg_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
