-- ══════════════════════════════════════════════════════════════════════
-- V5.10 — Alter orders table (Transactions refactor)
-- 1. order_date / shipping_date: DATE → TIMESTAMPTZ
-- 2. tracking_number: NOT NULL → NULL DEFAULT NULL
-- 3. DROP COLUMN device_session_id (loại bỏ phụ thuộc device_sessions)
-- 4. DROP COLUMN shop_discount_amount (không còn trong schema mới)
-- 5. ADD COLUMN updated_at + trigger
-- ══════════════════════════════════════════════════════════════════════

-- Step 1: Chuyển order_date từ DATE sang TIMESTAMPTZ
ALTER TABLE orders
    ALTER COLUMN order_date TYPE TIMESTAMPTZ
    USING order_date::TIMESTAMPTZ;

ALTER TABLE orders
    ALTER COLUMN order_date SET DEFAULT CURRENT_TIMESTAMP;

-- Step 2: Chuyển shipping_date từ DATE sang TIMESTAMPTZ
ALTER TABLE orders
    ALTER COLUMN shipping_date TYPE TIMESTAMPTZ
    USING shipping_date::TIMESTAMPTZ;

-- Step 3: tracking_number NOT NULL '' → NULL DEFAULT NULL
ALTER TABLE orders
    ALTER COLUMN tracking_number DROP NOT NULL,
    ALTER COLUMN tracking_number SET DEFAULT NULL;

-- Đặt lại các giá trị '' thành NULL (dữ liệu cũ)
UPDATE orders SET tracking_number = NULL WHERE tracking_number = '';

-- Step 4: DROP device_session_id
ALTER TABLE orders
    DROP COLUMN IF EXISTS device_session_id;

-- Step 5: DROP shop_discount_amount
ALTER TABLE orders
    DROP COLUMN IF EXISTS shop_discount_amount;

-- Step 6: ADD updated_at + trigger
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

UPDATE orders SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

CREATE TRIGGER trg_orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();
