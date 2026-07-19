-- =====================================================================
-- V2.9: Fix stock_quantity constraint để hỗ trợ -1 (vô cực/bán order)
--       và seed permissions cho Cart module
-- =====================================================================

-- 1. Xóa constraint cũ (chỉ cho phép >= 0) và tạo constraint mới
--    Cho phép: -1 (vô cực) hoặc >= 0 (số thực)
ALTER TABLE product_variants
DROP CONSTRAINT IF EXISTS chk_stock_valid;

ALTER TABLE product_variants
ADD CONSTRAINT chk_stock_valid CHECK (
    stock_quantity = -1
    OR stock_quantity >= 0
);
