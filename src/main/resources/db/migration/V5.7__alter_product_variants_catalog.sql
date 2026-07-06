-- ══════════════════════════════════════════════════════════════════════
-- V5.7 — Alter product_variants table (Catalog refactor)
-- 1. Add updated_at + trigger
-- 2. Add CHECK constraint: locked_stock <= stock_quantity
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: updated_at
ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

UPDATE product_variants SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

CREATE TRIGGER trg_product_variants_updated_at
BEFORE UPDATE ON product_variants
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- Step 3: Constraint đảm bảo locked_stock không vượt stock_quantity
-- (Bỏ qua nếu có row vi phạm từ dữ liệu cũ — dùng NOT VALID để thêm trước, validate sau)
ALTER TABLE product_variants
    ADD CONSTRAINT chk_stock_locked
    CHECK (stock_quantity >= locked_stock)
    NOT VALID;

-- Validate sau khi đã clean dữ liệu (chạy riêng nếu có dữ liệu cũ bẩn)
ALTER TABLE product_variants VALIDATE CONSTRAINT chk_stock_locked;
