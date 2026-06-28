-- V4.6__normalize_variant_attributes_keys.sql
-- Chuẩn hóa key trong cột variant_attributes (JSONB) của bảng product_variants:
--   'mau_sac' → 'color'
--   'kich_co' → 'size'
--
-- Sử dụng jsonb operator để rename key an toàn mà không mất data.
-- Áp dụng cho tất cả rows có key tiếng Việt.

UPDATE product_variants
SET variant_attributes = (variant_attributes - 'mau_sac' - 'kich_co')
    || jsonb_build_object(
        'color', variant_attributes->>'mau_sac',
        'size',  variant_attributes->>'kich_co'
    )
WHERE variant_attributes ? 'mau_sac'
   OR variant_attributes ? 'kich_co';
