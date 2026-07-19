INSERT INTO risk_rule_configs (rule_code, rule_name, risk_level, threshold_value, time_window_minutes) VALUES
('R1_MULTIPLE_DEVICES', 'Cảnh báo đăng nhập từ nhiều thiết bị khác nhau', 'HIGH', 3, 1440),
('R2_FAILED_PAYMENTS', 'Thanh toán thất bại liên tục', 'MEDIUM', 3, 10),
('R5_NEW_ACC_HIGH_VALUE', 'Tài khoản mới mua đơn hàng giá trị cao', 'CRITICAL', 5000000, NULL)
ON CONFLICT (rule_code) DO NOTHING;
