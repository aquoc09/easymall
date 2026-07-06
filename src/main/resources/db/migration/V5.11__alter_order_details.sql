-- ══════════════════════════════════════════════════════════════════════
-- V5.11 — Alter order_details table (Transactions refactor)
-- Tăng precision: DECIMAL(12,2) → NUMERIC(15,2) cho 2 cột tiền
-- ══════════════════════════════════════════════════════════════════════

ALTER TABLE order_details
    ALTER COLUMN order_detail_price TYPE NUMERIC(15, 2)
    USING order_detail_price::NUMERIC(15, 2);

ALTER TABLE order_details
    ALTER COLUMN total_money TYPE NUMERIC(15, 2)
    USING total_money::NUMERIC(15, 2);
