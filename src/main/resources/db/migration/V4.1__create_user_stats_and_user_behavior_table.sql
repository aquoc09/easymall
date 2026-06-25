-- 1. Bảng hành vi người dùng (Implicit Feedback cho Hybrid Recommendation)
CREATE TABLE IF NOT EXISTS user_behaviors (
    user_behavior_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NULL REFERENCES users (user_id) ON DELETE SET NULL, -- NULL nếu là Guest
    session_id VARCHAR(100) NOT NULL, -- Theo dõi phiên của Guest
    product_id BIGINT NULL REFERENCES products (product_id) ON DELETE SET NULL,
    category_id BIGINT NULL REFERENCES categories (category_id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL, -- 'VIEW', 'CLICK', 'ADD_TO_CART', 'REMOVE_FROM_CART', 'PURCHASE', 'RATE'
    keyword VARCHAR(255) NULL, -- Lưu từ khóa tìm kiếm dẫn tới hành vi
    context_data JSONB NULL, -- Lưu: {"device": "mobile", "duration_seconds": 45, "ip": "113.161.x.x"}
    variant_id BIGINT NULL REFERENCES product_variants (variant_id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_behavior_user ON user_behaviors (user_id);

CREATE INDEX IF NOT EXISTS idx_behavior_product ON user_behaviors (product_id);

CREATE INDEX IF NOT EXISTS idx_behavior_category ON user_behaviors (category_id);

CREATE INDEX IF NOT EXISTS idx_behavior_context ON user_behaviors USING GIN (context_data);

-- 4. Bảng tổng hợp chỉ số uy tín (XGBoost Feature Table)
CREATE TABLE IF NOT EXISTS user_stats (
    user_id BIGINT PRIMARY KEY REFERENCES users (user_id) ON DELETE CASCADE,
    total_orders INT DEFAULT 0,
    returned_orders_count INT DEFAULT 0,
    reputation_score DECIMAL(5, 2) DEFAULT 100.00,
    is_restricted BOOLEAN DEFAULT FALSE,
    account_age_days INT DEFAULT 0, -- Tài khoản clone mới tạo đi lừa đảo
    failed_payment_attempts_10m INT DEFAULT 0, -- Số lần thanh toán thất bại trong 10 phút (Dấu hiệu dò thẻ/Card testing)
    total_distinct_devices INT DEFAULT 1, -- Số lượng thiết bị login (1 tài khoản đăng nhập trên 50 máy = Tài khoản chiếm đoạt)
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_user_stats_updated_at
BEFORE UPDATE ON user_stats
FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();