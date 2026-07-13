-- Bước 1: Thêm các cột mới
ALTER TABLE user_behaviors
    ADD COLUMN IF NOT EXISTS duration_seconds INT NULL,
    ADD COLUMN IF NOT EXISTS source VARCHAR(50) NULL;

-- Bước 2: Migrate dữ liệu cũ (lấy duration_seconds từ trong context_data ra cột mới)
UPDATE user_behaviors
SET duration_seconds = CAST(context_data->>'duration_seconds' AS INT)
WHERE context_data ? 'duration_seconds' AND duration_seconds IS NULL;

-- Bước 3: Xóa indexes cũ không cần thiết / khác cấu trúc
DROP INDEX IF EXISTS idx_behavior_user;
DROP INDEX IF EXISTS idx_behavior_product;
DROP INDEX IF EXISTS idx_behavior_category;

-- Bước 4: Tạo indexes mới tối ưu cho query thời gian
CREATE INDEX IF NOT EXISTS idx_behavior_user_time    ON user_behaviors(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_behavior_product_time ON user_behaviors(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_behavior_action_time  ON user_behaviors(action_type, created_at DESC);
