CREATE TABLE IF NOT EXISTS coupons (
    coupon_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,

-- Loại giảm giá: 'PERCENTAGE' (theo %) hoặc 'FIXED_AMOUNT' (theo số tiền)
discount_type VARCHAR(20) NOT NULL,
discount_value NUMERIC(15, 2) NOT NULL,
max_discount_amount NUMERIC(15, 2) NULL, -- Giảm tối đa bao nhiêu tiền (nếu là dạng PERCENTAGE)
min_order_amount NUMERIC(15, 2) DEFAULT 0,

-- Giới hạn số lượng
max_usage INT DEFAULT 1000, -- Tổng lượt dùng toàn sàn
user_usage_limit INT DEFAULT 1, -- MỖI USER ĐƯỢC DÙNG TỐI ĐA MẤY LẦN (Rất quan trọng!)

-- Thời hạn
start_date TIMESTAMPTZ NOT NULL,
end_date TIMESTAMPTZ NOT NULL,
is_active BOOLEAN DEFAULT TRUE,

-- SỨC MẠNH LINH HOẠT: Điều kiện áp dụng nâng cao (Lưu dạng JSONB)
-- Ví dụ: {"applicable_category_ids": [1, 2, 3], "target_gender": 0}


applicable_conditions JSONB DEFAULT '{}'::jsonb, 
coupon_type VARCHAR(30) NOT NULL DEFAULT 'SHOP_VOUCHER',
    
created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP

);