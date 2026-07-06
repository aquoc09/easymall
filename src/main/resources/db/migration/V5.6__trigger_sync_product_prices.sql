-- ══════════════════════════════════════════════════════════════════════
-- V5.6 — DB Trigger: sync min_price / max_price on products
-- Trigger chạy AFTER INSERT/UPDATE/DELETE trên product_variants
-- Chỉ tính từ các variant is_active = TRUE
-- ══════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION sync_product_prices()
RETURNS trigger AS $$
DECLARE
    v_product_id BIGINT;
BEGIN
    -- Xác định product_id từ row thay đổi (NEW hoặc OLD tuỳ operation)
    v_product_id := COALESCE(NEW.product_id, OLD.product_id);

    UPDATE products
    SET
        min_price = COALESCE(
            (SELECT MIN(price) FROM product_variants
             WHERE product_id = v_product_id AND is_active = TRUE),
            0.00
        ),
        max_price = COALESCE(
            (SELECT MAX(price) FROM product_variants
             WHERE product_id = v_product_id AND is_active = TRUE),
            0.00
        )
    WHERE product_id = v_product_id;

    RETURN NULL; -- AFTER trigger, return value ignored
END;
$$ LANGUAGE plpgsql;

-- Gắn trigger vào product_variants
CREATE TRIGGER trg_sync_product_prices
AFTER INSERT OR UPDATE OF price, is_active OR DELETE
ON product_variants
FOR EACH ROW EXECUTE FUNCTION sync_product_prices();
