-- V6.3__alter_order_details_add_snapshot_columns.sql
ALTER TABLE order_details
ADD COLUMN product_name VARCHAR(255),
ADD COLUMN variant_attributes JSONB,
ADD COLUMN sku_code VARCHAR(50),
ADD COLUMN variant_image VARCHAR(500);

ALTER TABLE order_details ALTER COLUMN variant_id DROP NOT NULL;

-- Since the FK was created as "REFERENCES product_variants (variant_id) ON DELETE RESTRICT"
-- PostgreSQL autogenerates the name as "order_details_variant_id_fkey".
-- We need to drop it and recreate it with ON DELETE SET NULL.
ALTER TABLE order_details DROP CONSTRAINT IF EXISTS order_details_variant_id_fkey;

ALTER TABLE order_details ADD CONSTRAINT order_details_variant_id_fkey 
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id) ON DELETE SET NULL;
