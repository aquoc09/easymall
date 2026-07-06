-- ══════════════════════════════════════════════════════════════════════
-- V5.9 — Alter cart_items table (Cart refactor)
-- 1. Tăng precision total_money: DECIMAL(12,2) → NUMERIC(15,2)
-- 2. Add UNIQUE constraint (cart_id, variant_id) — tránh duplicate item
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: Đổi kiểu cột total_money
-- DECIMAL(12,2) giới hạn ~999 triệu; NUMERIC(15,2) hỗ trợ đến ~9.999 tỷ
-- phù hợp với giá variant có thể lên đến hàng chục triệu × số lượng lớn
ALTER TABLE cart_items
    ALTER COLUMN total_money TYPE NUMERIC(15, 2)
    USING total_money::NUMERIC(15, 2);

-- Step 2: Thêm UNIQUE constraint (cart_id, variant_id)
-- Đảm bảo mỗi variant chỉ xuất hiện 1 lần trong giỏ hàng.
-- NOT VALID: bỏ qua row cũ vi phạm (nếu có), validate sau khi clean data.
ALTER TABLE cart_items
    ADD CONSTRAINT uq_cart_items_cart_variant
    UNIQUE (cart_id, variant_id);
