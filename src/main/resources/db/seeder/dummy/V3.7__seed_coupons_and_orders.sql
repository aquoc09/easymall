-- ══════════════════════════════════════════════════════════════════════
-- V3.6: Seed Coupons + Orders + Order Details (Demo Data)
-- Dựa trên products/variants đã seed ở V2.7
-- Users giả định: seed user đầu tiên (user_id = 1) là ADMIN / người mua demo
-- ══════════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 1: COUPONS
-- ──────────────────────────────────────────────────────────────────────
-- 5 coupon đa dạng kịch bản: PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING,
--   PAYMENT_VOUCHER, và 1 coupon đã hết hạn để test error case.
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO
    coupons (
        code,
        description,
        discount_type,
        discount_value,
        max_discount_amount,
        min_order_amount,
        max_usage,
        user_usage_limit,
        start_date,
        end_date,
        is_active,
        coupon_type,
        applicable_conditions
    )
VALUES
    -- ─── 1. SUMMER20 — Giảm 20%, tối đa 100k, đơn từ 200k ────────────
    (
        'SUMMER20',
        'Summer Sale 2026 — Giảm 20% tối đa 100.000đ cho đơn từ 200.000đ',
        'PERCENTAGE',
        20.00,
        100000.00,
        200000.00,
        500,
        1,
        '2026-06-01 00:00:00+07',
        '2026-08-31 23:59:59+07',
        TRUE,
        'SHOP_VOUCHER',
        '{}'::jsonb
    ),

-- ─── 2. FREESHIP50 — Miễn phí ship cho đơn từ 150k ───────────────
(
    'FREESHIP50',
    'Miễn phí vận chuyển cho đơn hàng từ 150.000đ',
    'FIXED_AMOUNT',
    50000.00,
    NULL,
    150000.00,
    1000,
    2,
    '2026-06-01 00:00:00+07',
    '2026-12-31 23:59:59+07',
    TRUE,
    'FREE_SHIPPING',
    '{}'::jsonb
),

-- ─── 3. NEWUSER30K — Giảm 30k cố định, đơn từ 100k ───────────────
(
    'NEWUSER30K',
    'Ưu đãi khách hàng mới — Giảm 30.000đ cho đơn từ 100.000đ',
    'FIXED_AMOUNT',
    30000.00,
    NULL,
    100000.00,
    999,
    1,
    '2026-01-01 00:00:00+07',
    '2026-12-31 23:59:59+07',
    TRUE,
    'SHOP_VOUCHER',
    '{"user_usage_limit_note": "moi_tai_khoan_1_lan"}'::jsonb
),

-- ─── 4. PAY10 — Giảm 10% qua VNPAY/MoMo, tối đa 50k ────────────
(
    'PAY10',
    'Thanh toán online giảm 10% tối đa 50.000đ',
    'PERCENTAGE',
    10.00,
    50000.00,
    0.00,
    300,
    3,
    '2026-06-01 00:00:00+07',
    '2026-09-30 23:59:59+07',
    TRUE,
    'PAYMENT_VOUCHER',
    '{"payment_methods": ["VNPAY", "MOMO"]}'::jsonb
),

-- ─── 5. EXPIRED2024 — Mã hết hạn (dùng để test error case) ───────
(
    'EXPIRED2024',
    'Ma giam gia cuoi nam 2024 - da het han',
    'PERCENTAGE',
    15.00,
    75000.00,
    100000.00,
    200,
    1,
    '2024-11-01 00:00:00+07',
    '2024-12-31 23:59:59+07',
    FALSE,
    'SHOP_VOUCHER',
    '{}'::jsonb
) ON CONFLICT (code) DO NOTHING;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 2: ORDERS + ORDER DETAILS
-- Ghi chu: user_id va address_id duoc resolve bang subquery tu email
-- Phu thuoc vao V3.5 da seed buyer@easymall.vn va dia chi cua ho
-- ──────────────────────────────────────────────────────────────────────

-- ─── Order 1: DELIVERED — 2 áo thun basic, COD ───────────────────────
-- Tổng sản phẩm: 2 × 149.000 = 298.000đ | Ship: 30.000đ | Final: 328.000đ

INSERT INTO
    orders (
        user_id,
        address_id,
        device_session_id,
        order_date,
        order_status,
        payment_method,
        shipping_method,
        tracking_number,
        note,
        total_product_money,
        shop_discount_amount,
        original_shipping_fee,
        shipping_discount_amount,
        payment_discount_amount,
        final_payment_money
    )
VALUES (
        (SELECT user_id  FROM users     WHERE email = 'buyer@easymall.vn'),
        (SELECT address_id FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn') AND is_default = TRUE LIMIT 1),
        NULL,
        '2026-06-10',
        'DELIVERED',
        0, -- COD
        0, -- STANDARD
        'GHN-2026-001',
        'Giao buoi sang giup minh nhe',
        298000.00,
        0.00,
        30000.00,
        0.00,
        0.00,
        328000.00
    );

INSERT INTO
    order_details (
        order_id,
        variant_id,
        num_of_product,
        order_detail_price,
        total_money,
        item_status
    )
SELECT (
        SELECT order_id
        FROM orders
        WHERE
            tracking_number = 'GHN-2026-001'
    ), pv.variant_id, v.num_of_product, v.order_detail_price, v.num_of_product * v.order_detail_price, 'DELIVERED'
FROM product_variants pv
    JOIN (
        VALUES (
                'ATH-00001-TR-M-C3D4', 1, 149000
            ), (
                'ATH-00001-DE-L-M3N4', 1, 149000
            )
    ) AS v (
        sku_code, num_of_product, order_detail_price
    ) ON pv.sku_code = v.sku_code;

-- ─── Order 2: AWAITING_SHIPMENT — Áo oversize + quần short + mũ bucket, COD + SUMMER20 ──
-- Tổng SP: 229.000 + 229.000 + 159.000 = 617.000đ
-- SUMMER20: 20% = 123.400đ → capped 100.000đ
-- Ship: 30.000đ | Final: 547.000đ

INSERT INTO
    orders (
        user_id,
        address_id,
        device_session_id,
        order_date,
        order_status,
        payment_method,
        shipping_method,
        tracking_number,
        note,
        total_product_money,
        shop_discount_amount,
        original_shipping_fee,
        shipping_discount_amount,
        payment_discount_amount,
        final_payment_money
    )
VALUES (
        (SELECT user_id  FROM users     WHERE email = 'buyer@easymall.vn'),
        (SELECT address_id FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn') AND is_default = TRUE LIMIT 1),
        NULL,
        '2026-06-18',
        'AWAITING_SHIPMENT',
        0, -- COD
        0, -- STANDARD
        'GHN-2026-002',
        NULL,
        617000.00,
        100000.00, -- SUMMER20 giảm 20% capped 100k
        30000.00,
        0.00,
        0.00,
        547000.00
    );

INSERT INTO
    order_details (
        order_id,
        variant_id,
        num_of_product,
        order_detail_price,
        total_money,
        item_status
    )
SELECT (
        SELECT order_id
        FROM orders
        WHERE
            tracking_number = 'GHN-2026-002'
    ), pv.variant_id, v.num_of_product, v.order_detail_price, v.num_of_product * v.order_detail_price, 'AWAITING_SHIPMENT'
FROM product_variants pv
    JOIN (
        VALUES (
                'ATH-00002-DE-L-G3H4', 1, 229000
            ), (
                'QSK-00006-DE-30-W1X2', 1, 229000
            ), (
                'MBK-00007-DE-FS-A5B6', 1, 159000
            )
    ) AS v (
        sku_code, num_of_product, order_detail_price
    ) ON pv.sku_code = v.sku_code;

-- Ghi nhận coupon usage cho order 2
INSERT INTO
    coupon_usages (user_id, coupon_id, order_id)
VALUES (
        (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn'),
        (
            SELECT coupon_id
            FROM coupons
            WHERE
                code = 'SUMMER20'
        ),
        (
            SELECT order_id
            FROM orders
            WHERE
                tracking_number = 'GHN-2026-002'
        )
    ) ON CONFLICT DO NOTHING;

-- ─── Order 3: SHIPPING — Áo khoác bomber × 1, EXPRESS ────────────────
-- Tổng SP: 549.000đ | FREESHIP50 miễn phí ship | Ship gốc: 50.000đ
-- Final: 549.000đ

INSERT INTO
    orders (
        user_id,
        address_id,
        device_session_id,
        order_date,
        order_status,
        payment_method,
        shipping_method,
        tracking_number,
        note,
        total_product_money,
        shop_discount_amount,
        original_shipping_fee,
        shipping_discount_amount,
        payment_discount_amount,
        final_payment_money
    )
VALUES (
        (SELECT user_id  FROM users     WHERE email = 'buyer@easymall.vn'),
        (SELECT address_id FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn') AND is_default = TRUE LIMIT 1),
        NULL,
        '2026-06-20',
        'SHIPPING',
        0, -- COD
        1, -- EXPRESS
        'GHN-2026-003',
        'Giao hang nhanh trong ngay',
        549000.00,
        0.00,
        50000.00,
        50000.00, -- FREESHIP50 miễn phí toàn bộ phí ship
        0.00,
        549000.00
    );

INSERT INTO
    order_details (
        order_id,
        variant_id,
        num_of_product,
        order_detail_price,
        total_money,
        item_status
    )
SELECT (
        SELECT order_id
        FROM orders
        WHERE
            tracking_number = 'GHN-2026-003'
    ), pv.variant_id, v.num_of_product, v.order_detail_price, v.num_of_product * v.order_detail_price, 'SHIPPING'
FROM product_variants pv
    JOIN (
        VALUES (
                'AKH-00004-DE-L-G9H0', 1, 549000
            )
    ) AS v (
        sku_code, num_of_product, order_detail_price
    ) ON pv.sku_code = v.sku_code;

INSERT INTO
    coupon_usages (user_id, coupon_id, order_id)
VALUES (
        (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn'),
        (
            SELECT coupon_id
            FROM coupons
            WHERE
                code = 'FREESHIP50'
        ),
        (
            SELECT order_id
            FROM orders
            WHERE
                tracking_number = 'GHN-2026-003'
        )
    ) ON CONFLICT DO NOTHING;

-- ─── Order 4: PENDING — Quần jean × 2 size + áo sơ mi, PENDING (chờ xác nhận) ─
-- Tổng SP: 399.000 + 399.000 + 299.000 = 1.097.000đ
-- NEWUSER30K giảm 30.000đ cố định | Ship: 30.000đ | Final: 1.097.000đ

INSERT INTO
    orders (
        user_id,
        address_id,
        device_session_id,
        order_date,
        order_status,
        payment_method,
        shipping_method,
        tracking_number,
        note,
        total_product_money,
        shop_discount_amount,
        original_shipping_fee,
        shipping_discount_amount,
        payment_discount_amount,
        final_payment_money
    )
VALUES (
        (SELECT user_id  FROM users     WHERE email = 'buyer@easymall.vn'),
        (SELECT address_id FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn') AND is_default = TRUE LIMIT 1),
        NULL,
        '2026-06-22',
        'PENDING',
        0, -- COD
        0, -- STANDARD
        'ORD-PENDING-004',
        'Goi qua ho minh nhe',
        1097000.00,
        30000.00, -- NEWUSER30K
        30000.00,
        0.00,
        0.00,
        1097000.00
    );

INSERT INTO
    order_details (
        order_id,
        variant_id,
        num_of_product,
        order_detail_price,
        total_money,
        item_status
    )
SELECT (
        SELECT order_id
        FROM orders
        WHERE
            tracking_number = 'ORD-PENDING-004'
    ), pv.variant_id, v.num_of_product, v.order_detail_price, v.num_of_product * v.order_detail_price, 'PENDING'
FROM product_variants pv
    JOIN (
        VALUES (
                'QJS-00005-XD-30-A9B0', 1, 399000
            ), (
                'QJS-00005-DE-32-I7J8', 1, 399000
            ), (
                'ASM-00003-TR-M-Q3R4', 1, 299000
            )
    ) AS v (
        sku_code, num_of_product, order_detail_price
    ) ON pv.sku_code = v.sku_code;

INSERT INTO
    coupon_usages (user_id, coupon_id, order_id)
VALUES (
        (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn'),
        (
            SELECT coupon_id
            FROM coupons
            WHERE
                code = 'NEWUSER30K'
        ),
        (
            SELECT order_id
            FROM orders
            WHERE
                tracking_number = 'ORD-PENDING-004'
        )
    ) ON CONFLICT DO NOTHING;

-- ─── Order 5: CANCELLED — Áo thun cổ V × 2, đã hủy ─────────────────
-- Không có coupon | locked_stock đã được giải phóng

INSERT INTO
    orders (
        user_id,
        address_id,
        device_session_id,
        order_date,
        order_status,
        payment_method,
        shipping_method,
        tracking_number,
        note,
        total_product_money,
        shop_discount_amount,
        original_shipping_fee,
        shipping_discount_amount,
        payment_discount_amount,
        final_payment_money
    )
VALUES (
        (SELECT user_id  FROM users     WHERE email = 'buyer@easymall.vn'),
        (SELECT address_id FROM addresses WHERE user_id = (SELECT user_id FROM users WHERE email = 'buyer@easymall.vn') AND is_default = TRUE LIMIT 1),
        NULL,
        '2026-06-15',
        'CANCELLED',
        0, -- COD
        0, -- STANDARD
        'ORD-CANCEL-005',
        'Dat nham size xin huy',
        358000.00,
        0.00,
        30000.00,
        0.00,
        0.00,
        388000.00
    );

INSERT INTO
    order_details (
        order_id,
        variant_id,
        num_of_product,
        order_detail_price,
        total_money,
        item_status
    )
SELECT (
        SELECT order_id
        FROM orders
        WHERE
            tracking_number = 'ORD-CANCEL-005'
    ), pv.variant_id, v.num_of_product, v.order_detail_price, v.num_of_product * v.order_detail_price, 'CANCELLED'
FROM product_variants pv
    JOIN (
        VALUES (
                'ATV-00008-TR-M-M7N8', 1, 179000
            ), (
                'ATV-00008-DE-L-W7X8', 1, 179000
            )
    ) AS v (
        sku_code, num_of_product, order_detail_price
    ) ON pv.sku_code = v.sku_code;