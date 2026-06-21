-- ══════════════════════════════════════════════════════════════════════
-- V2.7: Seed Categories + Products + Variants + Images (Demo Data)
-- Image prefix: https://shopco-s3.s3.ap-southeast-1.amazonaws.com/
-- ══════════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 1: CATEGORIES (3 cấp — thời trang)
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO
    categories (
        category_code,
        category_name,
        category_status,
        level,
        parent_id,
        icon_url,
        target_demographic,
        category_type,
        display_order
    )
VALUES
    -- Level 1 — Root categories
    (
        'ao',
        'Áo',
        1,
        1,
        NULL,
        NULL,
        NULL,
        'STANDARD',
        1
    ),
    (
        'quan',
        'Quần',
        1,
        1,
        NULL,
        NULL,
        NULL,
        'STANDARD',
        2
    ),
    (
        'phu-kien',
        'Phụ Kiện',
        1,
        1,
        NULL,
        NULL,
        NULL,
        'STANDARD',
        3
    ) ON CONFLICT (category_code) DO NOTHING;

INSERT INTO
    categories (
        category_code,
        category_name,
        category_status,
        level,
        parent_id,
        icon_url,
        target_demographic,
        category_type,
        display_order
    )
VALUES
    -- Level 2 — Sub-categories của "Áo"
    (
        'ao-thun',
        'Áo Thun',
        1,
        2,
        (
            SELECT category_id
            FROM categories
            WHERE
                category_code = 'ao'
        ),
        NULL,
        NULL,
        'STANDARD',
        1
    ),
    (
        'ao-khoac',
        'Áo Khoác',
        1,
        2,
        (
            SELECT category_id
            FROM categories
            WHERE
                category_code = 'ao'
        ),
        NULL,
        NULL,
        'STANDARD',
        2
    ),
    (
        'ao-so-mi',
        'Áo Sơ Mi',
        1,
        2,
        (
            SELECT category_id
            FROM categories
            WHERE
                category_code = 'ao'
        ),
        NULL,
        NULL,
        'STANDARD',
        3
    ),

-- Level 2 — Sub-categories của "Quần"
(
    'quan-jean',
    'Quần Jean',
    1,
    2,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'quan'
    ),
    NULL,
    NULL,
    'STANDARD',
    1
),
(
    'quan-short',
    'Quần Short',
    1,
    2,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'quan'
    ),
    NULL,
    NULL,
    'STANDARD',
    2
),

-- Level 2 — Sub-categories của "Phụ Kiện"
(
    'mu-non',
    'Mũ / Nón',
    1,
    2,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'phu-kien'
    ),
    NULL,
    NULL,
    'STANDARD',
    1
),
(
    'that-lung',
    'Thắt Lưng',
    1,
    2,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'phu-kien'
    ),
    NULL,
    NULL,
    'STANDARD',
    2
) ON CONFLICT (category_code) DO NOTHING;

INSERT INTO
    categories (
        category_code,
        category_name,
        category_status,
        level,
        parent_id,
        icon_url,
        target_demographic,
        category_type,
        display_order
    )
VALUES
    -- Level 3 — Sub-sub-categories của "Áo Thun"
    (
        'ao-thun-co-tron',
        'Áo Thun Cổ Tròn',
        1,
        3,
        (
            SELECT category_id
            FROM categories
            WHERE
                category_code = 'ao-thun'
        ),
        NULL,
        NULL,
        'STANDARD',
        1
    ),
    (
        'ao-thun-co-v',
        'Áo Thun Cổ V',
        1,
        3,
        (
            SELECT category_id
            FROM categories
            WHERE
                category_code = 'ao-thun'
        ),
        NULL,
        NULL,
        'STANDARD',
        2
    ),

-- Level 3 — Sub-sub-categories của "Quần Jean"
(
    'quan-jean-slim',
    'Quần Jean Slim Fit',
    1,
    3,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'quan-jean'
    ),
    NULL,
    NULL,
    'STANDARD',
    1
),
(
    'quan-jean-rong',
    'Quần Jean Rộng',
    1,
    3,
    (
        SELECT category_id
        FROM categories
        WHERE
            category_code = 'quan-jean'
    ),
    NULL,
    NULL,
    'STANDARD',
    2
) ON CONFLICT (category_code) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 2: PRODUCTS
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO products (
    product_slug, product_name, product_description,
    in_popular, in_stock, target_gender, max_order_quantity,
    options_config, product_tags, category_id,
    weight_kg, length_m, width_m, height_m
)
VALUES
    -- ─── Product 1: Áo Thun Basic ────────────────────────────────────
    (
        'ao-thun-basic-cotton-unisex',
        'Áo Thun Basic Cotton Unisex',
        'Áo thun cổ tròn chất liệu cotton 100% thoáng mát, thiết kế basic phù hợp mọi phong cách. Dễ phối đồ, bền màu sau nhiều lần giặt.',
        TRUE, TRUE, 2, 10,
        '{"sizes": ["S","M","L","XL","2XL"], "colors": ["Trắng","Đen","Xám","Navy","Đỏ"]}',
        '["cotton", "basic", "unisex", "thoáng mát"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'ao-thun-co-tron'),
        0.20, 0.30, 0.25, 0.02
    ),

-- ─── Product 2: Áo Thun Oversize ─────────────────────────────────
(
        'ao-thun-oversize-graphic',
        'Áo Thun Oversize Graphic Tee',
        'Áo thun form rộng oversize với print đồ hoạ độc đáo. Chất liệu cotton co giãn 4 chiều, kiểu dáng thoải mái phù hợp xu hướng streetwear.',
        TRUE, TRUE, 2, 8,
        '{"sizes": ["M","L","XL","2XL"], "colors": ["Trắng","Đen","Beige"]}',
        '["oversize", "graphic", "streetwear", "unisex", "trendy"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'ao-thun-co-tron'),
        0.25, 0.35, 0.30, 0.02
    ),

-- ─── Product 3: Áo Sơ Mi Trơn ────────────────────────────────────
(
        'ao-so-mi-tron-dai-tay',
        'Áo Sơ Mi Trơn Dài Tay',
        'Áo sơ mi dài tay chất liệu lụa mềm mịn, ít nhăn. Thiết kế slim fit phù hợp đi làm, đi học hoặc các dịp bán trang trọng.',
        FALSE, TRUE, 2, 5,
        '{"sizes": ["S","M","L","XL"], "colors": ["Trắng","Xanh nhạt","Kem"]}',
        '["so-mi", "dai-tay", "slim-fit", "di-lam", "lich-su"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'ao-so-mi'),
        0.25, 0.35, 0.30, 0.02
    ),

-- ─── Product 4: Áo Khoác Bomber ──────────────────────────────────
(
        'ao-khoac-bomber-nam',
        'Áo Khoác Bomber Nam',
        'Áo khoác bomber form đứng, chất liệu polyester chống gió nhẹ, lót lông ấm áp. Thiết kế cổ điển với túi hộp hai bên tiện lợi.',
        TRUE, TRUE, 1, 5,
        '{"sizes": ["M","L","XL","2XL"], "colors": ["Đen","Xanh Army","Nâu"]}',
        '["bomber", "khoac", "nam", "chong-gio", "mua-dong"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'ao-khoac'),
        0.70, 0.45, 0.35, 0.05
    ),

-- ─── Product 5: Quần Jean Slim ────────────────────────────────────
(
        'quan-jean-slim-fit-co-dan',
        'Quần Jean Slim Fit Co Dãn',
        'Quần jean form slim fit chất liệu denim co giãn 2 chiều, thoải mái trong mọi tư thế. Thiết kế 5 túi kinh điển, bền màu sau nhiều lần giặt.',
        TRUE, TRUE, 1, 5,
        '{"sizes": ["28","29","30","31","32","33","34"], "colors": ["Xanh Nhạt","Xanh Đậm","Đen"]}',
        '["jean", "slim-fit", "co-dan", "nam", "basic"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'quan-jean-slim'),
        0.60, 0.40, 0.35, 0.05
    ),

-- ─── Product 6: Quần Short Kaki ──────────────────────────────────
(
        'quan-short-kaki-basic',
        'Quần Short Kaki Basic',
        'Quần short kaki phong cách basic, chất liệu cotton kaki mềm mịn thoáng mát. Phù hợp đi chơi, dạo phố, hoạt động ngoài trời mùa hè.',
        FALSE, TRUE, 2, 10,
        '{"sizes": ["28","29","30","31","32","33"], "colors": ["Be","Xanh Rêu","Đen","Nâu"]}',
        '["short", "kaki", "mua-he", "thoang-mat", "basic"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'quan-short'),
        0.35, 0.30, 0.25, 0.03
    ),

-- ─── Product 7: Mũ Bucket ─────────────────────────────────────────
(
        'mu-bucket-cotton-unisex',
        'Mũ Bucket Cotton Unisex',
        'Mũ bucket chất liệu cotton cao cấp, thiết kế unisex phù hợp nam nữ. Vành mũ mềm linh hoạt, có thể gập lại dễ mang theo.',
        TRUE, TRUE, 2, 10,
        '{"sizes": ["Free Size"], "colors": ["Đen","Trắng","Beige","Xanh Navy","Hồng"]}',
        '["mu", "bucket", "unisex", "streetwear", "phu-kien"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'mu-non'),
        0.10, 0.25, 0.25, 0.10
    ),

-- ─── Product 8: Áo Thun Cổ V ─────────────────────────────────────
(
        'ao-thun-co-v-premium',
        'Áo Thun Cổ V Premium Cotton',
        'Áo thun cổ V chất liệu cotton 100% cao cấp, phần cổ V vừa phải thanh lịch. Đường may chắc chắn, bền màu, form đứng thoải mái.',
        FALSE, TRUE, 2, 10,
        '{"sizes": ["S","M","L","XL","2XL"], "colors": ["Trắng","Đen","Xám","Camel"]}',
        '["co-v", "cotton", "premium", "basic", "unisex"]'::jsonb,
        (SELECT category_id FROM categories WHERE category_code = 'ao-thun-co-v'),
        0.20, 0.30, 0.25, 0.02
    )
ON CONFLICT (product_slug) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 3: PRODUCT VARIANTS
-- key: product_slug → product_id dùng subquery
-- ──────────────────────────────────────────────────────────────────────

-- ─── Variants: Áo Thun Basic ─────────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (149000, 65000, '{"mau_sac":"Trắng","kich_co":"S"}',   'ATH-00001-TR-S-A1B2',  50),
    (149000, 65000, '{"mau_sac":"Trắng","kich_co":"M"}',   'ATH-00001-TR-M-C3D4',  80),
    (149000, 65000, '{"mau_sac":"Trắng","kich_co":"L"}',   'ATH-00001-TR-L-E5F6',  70),
    (149000, 65000, '{"mau_sac":"Trắng","kich_co":"XL"}',  'ATH-00001-TR-XL-G7H8', 40),
    (149000, 65000, '{"mau_sac":"Đen","kich_co":"S"}',     'ATH-00001-DE-S-I9J0',  45),
    (149000, 65000, '{"mau_sac":"Đen","kich_co":"M"}',     'ATH-00001-DE-M-K1L2',  90),
    (149000, 65000, '{"mau_sac":"Đen","kich_co":"L"}',     'ATH-00001-DE-L-M3N4',  75),
    (149000, 65000, '{"mau_sac":"Đen","kich_co":"XL"}',    'ATH-00001-DE-XL-O5P6', 35),
    (149000, 65000, '{"mau_sac":"Xám","kich_co":"M"}',     'ATH-00001-XA-M-Q7R8',  60),
    (149000, 65000, '{"mau_sac":"Xám","kich_co":"L"}',     'ATH-00001-XA-L-S9T0',  55),
    (149000, 65000, '{"mau_sac":"Navy","kich_co":"M"}',    'ATH-00001-NA-M-U1V2',  40),
    (149000, 65000, '{"mau_sac":"Navy","kich_co":"L"}',    'ATH-00001-NA-L-W3X4',  40)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'ao-thun-basic-cotton-unisex'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Áo Thun Oversize Graphic ──────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (229000, 100000, '{"mau_sac":"Trắng","kich_co":"M"}',  'ATH-00002-TR-M-Y5Z6',  30),
    (229000, 100000, '{"mau_sac":"Trắng","kich_co":"L"}',  'ATH-00002-TR-L-A7B8',  35),
    (229000, 100000, '{"mau_sac":"Trắng","kich_co":"XL"}', 'ATH-00002-TR-XL-C9D0', 25),
    (229000, 100000, '{"mau_sac":"Đen","kich_co":"M"}',    'ATH-00002-DE-M-E1F2',  40),
    (229000, 100000, '{"mau_sac":"Đen","kich_co":"L"}',    'ATH-00002-DE-L-G3H4',  45),
    (229000, 100000, '{"mau_sac":"Đen","kich_co":"XL"}',   'ATH-00002-DE-XL-I5J6', 20),
    (229000, 100000, '{"mau_sac":"Beige","kich_co":"M"}',  'ATH-00002-BE-M-K7L8',  25),
    (229000, 100000, '{"mau_sac":"Beige","kich_co":"L"}',  'ATH-00002-BE-L-M9N0',  30)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'ao-thun-oversize-graphic'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Áo Sơ Mi Trơn Dài Tay ────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (299000, 140000, '{"mau_sac":"Trắng","kich_co":"S"}',       'ASM-00003-TR-S-O1P2',  20),
    (299000, 140000, '{"mau_sac":"Trắng","kich_co":"M"}',       'ASM-00003-TR-M-Q3R4',  30),
    (299000, 140000, '{"mau_sac":"Trắng","kich_co":"L"}',       'ASM-00003-TR-L-S5T6',  25),
    (299000, 140000, '{"mau_sac":"Trắng","kich_co":"XL"}',      'ASM-00003-TR-XL-U7V8', 15),
    (299000, 140000, '{"mau_sac":"Xanh nhạt","kich_co":"M"}',   'ASM-00003-XN-M-W9X0',  20),
    (299000, 140000, '{"mau_sac":"Xanh nhạt","kich_co":"L"}',   'ASM-00003-XN-L-Y1Z2',  20),
    (299000, 140000, '{"mau_sac":"Kem","kich_co":"M"}',         'ASM-00003-KE-M-A3B4',  15),
    (299000, 140000, '{"mau_sac":"Kem","kich_co":"L"}',         'ASM-00003-KE-L-C5D6',  15)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'ao-so-mi-tron-dai-tay'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Áo Khoác Bomber ───────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (549000, 250000, '{"mau_sac":"Đen","kich_co":"M"}',       'AKH-00004-DE-M-E7F8',  15),
    (549000, 250000, '{"mau_sac":"Đen","kich_co":"L"}',       'AKH-00004-DE-L-G9H0',  20),
    (549000, 250000, '{"mau_sac":"Đen","kich_co":"XL"}',      'AKH-00004-DE-XL-I1J2', 10),
    (549000, 250000, '{"mau_sac":"Xanh Army","kich_co":"M"}', 'AKH-00004-XA-M-K3L4',  12),
    (549000, 250000, '{"mau_sac":"Xanh Army","kich_co":"L"}', 'AKH-00004-XA-L-M5N6',  15),
    (549000, 250000, '{"mau_sac":"Nâu","kich_co":"M"}',       'AKH-00004-NA-M-O7P8',  10),
    (549000, 250000, '{"mau_sac":"Nâu","kich_co":"L"}',       'AKH-00004-NA-L-Q9R0',  10)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'ao-khoac-bomber-nam'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Quần Jean Slim Fit ────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (399000, 180000, '{"mau_sac":"Xanh Nhạt","kich_co":"28"}', 'QJS-00005-XN-28-S1T2', 20),
    (399000, 180000, '{"mau_sac":"Xanh Nhạt","kich_co":"30"}', 'QJS-00005-XN-30-U3V4', 25),
    (399000, 180000, '{"mau_sac":"Xanh Nhạt","kich_co":"32"}', 'QJS-00005-XN-32-W5X6', 20),
    (399000, 180000, '{"mau_sac":"Xanh Đậm","kich_co":"28"}',  'QJS-00005-XD-28-Y7Z8', 15),
    (399000, 180000, '{"mau_sac":"Xanh Đậm","kich_co":"30"}',  'QJS-00005-XD-30-A9B0', 25),
    (399000, 180000, '{"mau_sac":"Xanh Đậm","kich_co":"32"}',  'QJS-00005-XD-32-C1D2', 20),
    (399000, 180000, '{"mau_sac":"Đen","kich_co":"28"}',       'QJS-00005-DE-28-E3F4', 15),
    (399000, 180000, '{"mau_sac":"Đen","kich_co":"30"}',       'QJS-00005-DE-30-G5H6', 20),
    (399000, 180000, '{"mau_sac":"Đen","kich_co":"32"}',       'QJS-00005-DE-32-I7J8', 15)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'quan-jean-slim-fit-co-dan'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Quần Short Kaki ───────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (229000, 95000, '{"mau_sac":"Be","kich_co":"28"}',      'QSK-00006-BE-28-K9L0', 30),
    (229000, 95000, '{"mau_sac":"Be","kich_co":"30"}',      'QSK-00006-BE-30-M1N2', 35),
    (229000, 95000, '{"mau_sac":"Be","kich_co":"32"}',      'QSK-00006-BE-32-O3P4', 25),
    (229000, 95000, '{"mau_sac":"Xanh Rêu","kich_co":"28"}','QSK-00006-XR-28-Q5R6', 20),
    (229000, 95000, '{"mau_sac":"Xanh Rêu","kich_co":"30"}','QSK-00006-XR-30-S7T8', 25),
    (229000, 95000, '{"mau_sac":"Đen","kich_co":"28"}',     'QSK-00006-DE-28-U9V0', 20),
    (229000, 95000, '{"mau_sac":"Đen","kich_co":"30"}',     'QSK-00006-DE-30-W1X2', 25),
    (229000, 95000, '{"mau_sac":"Đen","kich_co":"32"}',     'QSK-00006-DE-32-Y3Z4', 20)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'quan-short-kaki-basic'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Mũ Bucket ─────────────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (159000, 60000, '{"mau_sac":"Đen","kich_co":"Free Size"}',      'MBK-00007-DE-FS-A5B6', 40),
    (159000, 60000, '{"mau_sac":"Trắng","kich_co":"Free Size"}',    'MBK-00007-TR-FS-C7D8', 35),
    (159000, 60000, '{"mau_sac":"Beige","kich_co":"Free Size"}',    'MBK-00007-BE-FS-E9F0', 30),
    (159000, 60000, '{"mau_sac":"Xanh Navy","kich_co":"Free Size"}','MBK-00007-XN-FS-G1H2', 25),
    (159000, 60000, '{"mau_sac":"Hồng","kich_co":"Free Size"}',     'MBK-00007-HO-FS-I3J4', 20)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'mu-bucket-cotton-unisex'
ON CONFLICT (sku_code) DO NOTHING;

-- ─── Variants: Áo Thun Cổ V ──────────────────────────────────────────
INSERT INTO product_variants (product_id, price, cost_price, variant_attributes, sku_code, stock_quantity, is_active, locked_stock)
SELECT p.product_id,
       v.price, v.cost_price, v.variant_attributes::jsonb, v.sku_code, v.stock_quantity, TRUE, 0
FROM products p
CROSS JOIN (VALUES
    (179000, 75000, '{"mau_sac":"Trắng","kich_co":"S"}',  'ATV-00008-TR-S-K5L6',  30),
    (179000, 75000, '{"mau_sac":"Trắng","kich_co":"M"}',  'ATV-00008-TR-M-M7N8',  45),
    (179000, 75000, '{"mau_sac":"Trắng","kich_co":"L"}',  'ATV-00008-TR-L-O9P0',  40),
    (179000, 75000, '{"mau_sac":"Trắng","kich_co":"XL"}', 'ATV-00008-TR-XL-Q1R2', 25),
    (179000, 75000, '{"mau_sac":"Đen","kich_co":"S"}',    'ATV-00008-DE-S-S3T4',  25),
    (179000, 75000, '{"mau_sac":"Đen","kich_co":"M"}',    'ATV-00008-DE-M-U5V6',  40),
    (179000, 75000, '{"mau_sac":"Đen","kich_co":"L"}',    'ATV-00008-DE-L-W7X8',  35),
    (179000, 75000, '{"mau_sac":"Xám","kich_co":"M"}',    'ATV-00008-XA-M-Y9Z0',  30),
    (179000, 75000, '{"mau_sac":"Camel","kich_co":"M"}',  'ATV-00008-CA-M-A1B2',  20)
) AS v(price, cost_price, variant_attributes, sku_code, stock_quantity)
WHERE p.product_slug = 'ao-thun-co-v-premium'
ON CONFLICT (sku_code) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 4: PRODUCT IMAGES
-- Prefix: https://shopco-s3.s3.ap-southeast-1.amazonaws.com/
-- Tất cả dùng chung placeholder: products/7FHnMT7YuxuPdJVDi2rmjTigpaozENfSZiP2ESaU.png
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO
    product_images (
        product_id,
        image_url,
        is_thumbnail,
        display_order
    )
SELECT p.product_id, img.image_url, img.is_thumbnail, img.display_order
FROM products p
    CROSS JOIN (
        VALUES (
                'products/7FHnMT7YuxuPdJVDi2rmjTigpaozENfSZiP2ESaU.png', TRUE, 1
            ), (
                'products/7FHnMT7YuxuPdJVDi2rmjTigpaozENfSZiP2ESaU.png', FALSE, 2
            ), (
                'products/7FHnMT7YuxuPdJVDi2rmjTigpaozENfSZiP2ESaU.png', FALSE, 3
            )
    ) AS img (
        image_url, is_thumbnail, display_order
    )
WHERE
    p.product_slug IN (
        'ao-thun-basic-cotton-unisex',
        'ao-thun-oversize-graphic',
        'ao-so-mi-tron-dai-tay',
        'ao-khoac-bomber-nam',
        'quan-jean-slim-fit-co-dan',
        'quan-short-kaki-basic',
        'mu-bucket-cotton-unisex',
        'ao-thun-co-v-premium'
    );