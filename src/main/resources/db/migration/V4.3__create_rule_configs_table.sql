-- 1. Thêm bảng cấu hình Threshold động cho Rule Engine
CREATE TABLE IF NOT EXISTS fraud_rule_configs (
    config_id INT PRIMARY KEY,
    review_threshold DECIMAL(5, 2) NOT NULL DEFAULT 40.00,
    decline_threshold DECIMAL(5, 2) NOT NULL DEFAULT 75.00,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users (user_id)
);

INSERT INTO
    fraud_rule_configs (
        config_id,
        review_threshold,
        decline_threshold
    )
VALUES (1, 40.00, 75.00);

-- 2. Cập nhật bảng fraud_records_and_labels để nhận SHAP factors
ALTER TABLE fraud_records_and_labels
ADD COLUMN top_risk_factors JSONB NULL;
-- VD: ["order_total_amount > 50M", "is_vpn_proxy = 1"]