## **1. TỔNG QUAN KIẾN TRÚC VÀ LUỒNG DỮ LIỆU**
Module Phát hiện gian lận hoạt động theo cơ chế **Real-time Scoring (Chấm điểm thời gian thực)** kết hợp **Continuous Learning (Học liên tục)**.

1. **Thu thập (Tracking):** Backend ghi nhận dấu vết thiết bị vào bảng `device_sessions` khi user truy cập và cập nhật liên tục các chỉ số uy tín vào bảng `user_stats`.
2. **Ra quyết định thời gian thực (Real-time Prediction):** Ngay khi user bấm "Thanh toán", Backend đóng gói dữ liệu hiện tại gửi sang ML Service (Python). Mô hình XGBoost trả về `risk_score` (0-100).
3. **Phân luồng đơn hàng:** Dựa vào `risk_score`, hệ thống sẽ APPROVE (Cho qua), REVIEW (Tạm giữ chờ duyệt), hoặc DECLINE (Từ chối). Kết quả này lưu vào bảng `fraud_records_and_labels`.
4. **Vòng lặp phản hồi (Feedback Loop):** Admin duyệt các đơn bị REVIEW và gán nhãn thực tế (`final_label`). Định kỳ, mô hình lấy dữ liệu mới đã dán nhãn để retrain (huấn luyện lại).

## **2. QUY TRÌNH TRÍCH XUẤT ĐẶC TRƯNG (FEATURE ENGINEERING)**
Thuật toán XGBoost là dạng học có giám sát (Supervised Learning), nó cần các đầu vào (Features) dạng số hoặc phân loại. Từ 2 bảng `device_sessions` và `user_stats`, chúng ta trích xuất ra Vector Đặc Trưng cho mỗi giao dịch:

### **2.1. Nhóm Đặc trưng Thiết bị & Mạng (Device & Network Features)**
- **is_vpn_proxy (Boolean -> 0/1):** Đánh dấu IP có thuộc dải bẩn/ẩn danh hay không. Trọng số rủi ro rất cao.
- **device_fingerprint_match (0/1):** Kiểm tra mã hash thiết bị lúc đặt hàng có khớp với lịch sử thiết bị hay dùng không.
- **location_mismatch (0/1):** Địa chỉ IP lúc thanh toán có khác biệt bất thường với địa chỉ giao hàng không.
- *Lưu ý:* Việc thu thập `device_fingerprint` (Canvas/WebGL) từ Frontend cần được mã hóa an toàn trước khi lưu.

### **2.2. Nhóm Đặc trưng Hành vi & Uy tín (Behavioral & Reputation Features)**
- **account_age_days (Numeric):** Tuổi tài khoản. Tài khoản mới tạo (< 1 ngày) có nguy cơ gian lận cao hơn.
- **reputation_score (Numeric):** Điểm uy tín do hệ thống tự tính (ví dụ: mặc định 100, trừ điểm nếu bom hàng).
- **failed_payment_attempts_10m (Numeric):** Bắt hành vi Card Testing (Dò thẻ tín dụng ăn cắp). Nếu chỉ số này > 3, rủi ro cực kỳ lớn.
- **total_distinct_devices (Numeric):** Số lượng thiết bị login. Bắt hành vi Account Takeover (Tài khoản bị hack và bán lại, đăng nhập từ nhiều nơi).
- **return_rate (Numeric):** Tỷ lệ hoàn trả/hủy đơn = returned_orders_count / total_orders.

## **3. NGHIỆP VỤ HUẤN LUYỆN MÔ HÌNH (TRAINING PIPELINE)**

### **3.1. Xác định Nhãn dữ liệu (Target Variable - Y)**
Mô hình cần học để phân loại đơn hàng là Gian lận (Fraud - 1) hay Sạch (Legit - 0). Nhãn này lấy từ cột `final_label` trong bảng `fraud_records_and_labels`:
- **Y = 0 (Legit):** Khi `final_label` = 'ecommerce_marked_as_approved' hoặc hoàn tất thanh toán bình thường.
- **Y = 1 (Fraud):** Khi `final_label` = 'ecommerce_chargeback_fraud' (Bị ngân hàng đòi tiền lại) hoặc `admin_rejected_fraud`.

### **3.2. Quá trình Train (XGBoost Classifier)**
- **Xử lý mất cân bằng dữ liệu (Imbalanced Data):** Trong thực tế, đơn lừa đảo thường chỉ chiếm < 2% tổng số đơn. Cần áp dụng kỹ thuật **SMOTE** (Synthetic Minority Over-sampling Technique) hoặc cấu hình tham số `scale_pos_weight` trong XGBoost để mô hình không bị "mù" trước các ca gian lận.
- **Metric đánh giá:** Không dùng Accuracy (Độ chính xác tổng thể). Bắt buộc phải dùng **Precision, Recall, và F1-Score**. Đặc biệt ưu tiên **Recall** để thà bắt nhầm (báo động giả - đưa vào diện REVIEW) còn hơn bỏ sót (để lọt đơn gian lận).

## **4. CHIẾN LƯỢC RA QUYẾT ĐỊNH & PHỤC VỤ (REAL-TIME SERVING)**
Khi Frontend gọi API Checkout (POST /api/orders/checkout), luồng Backend và ML Service diễn ra như sau:

1. Backend tổng hợp các Feature hiện tại của user, gọi HTTP POST sang ML Service (Python).
2. Mô hình XGBoost tính toán và trả về `risk_score` (từ 0.00 đến 100.00).
3. Backend áp dụng Rule-based để xử lý luồng đơn hàng:

| Ngưỡng Risk Score | Quyết định (System Decision) | Hành động của Hệ thống (Action) |
| :--- | :--- | :--- |
| **0 - 40** | APPROVE (An toàn) | Cho phép thanh toán thành công, chuyển trạng thái đơn hàng sang CONFIRMED. |
| **41 - 75** | REVIEW (Nghi ngờ) | Cho phép thanh toán, nhưng đưa đơn vào trạng thái PENDING_REVIEW (Đóng băng doanh thu). Cảnh báo lên Admin Dashboard. |
| **> 75** | DECLINE (Nguy cơ cao) | Chặn ngay lập tức. Báo lỗi "Giao dịch bị từ chối vì lý do bảo mật". Đánh dấu tài khoản `is_restricted` = TRUE trong bảng `user_stats`. |

## **5. MLOps: VÒNG LẶP PHẢN HỒI (FEEDBACK LOOP)**
Bảng `fraud_records_and_labels` là trái tim của hệ thống học máy liên tục.
- **Duyệt thủ công:** Hằng ngày, Quản trị viên (Admin) vào Dashboard kiểm tra các đơn bị gán mác REVIEW (kèm theo `analyst_notes` giải thích vì sao hệ thống nghi ngờ).
- **Gán nhãn lại:** Admin sẽ quyết định đơn này là "Sạch" hay "Gian lận thực sự", và cập nhật vào cột `final_label`. Thao tác này cung cấp Ground Truth mới và chính xác.
- **Retrain (Định kỳ):** Mỗi tuần một lần, một Cronjob sẽ gom toàn bộ dữ liệu mới đã được Admin gán nhãn để huấn luyện lại mô hình XGBoost, giúp mô hình thông minh hơn và giảm dần tỷ lệ báo động giả (False Positives) trong tương lai.

---

# **TÀI LIỆU ĐẶC TẢ TRIỂN KHAI (IMPLEMENTATION SPECIFICATION)**

## **Dự án: EasyMall - Fraud Detection AI Module**
Tài liệu này tổng hợp toàn bộ các luồng nghiệp vụ, cấu trúc dữ liệu và API cần thiết. Blueprint này được thiết kế sát với thực tế mã nguồn để giúp quá trình code diễn ra liền mạch, đảm bảo hệ thống đạt được những **tiến triển tốt** trong giai đoạn vận hành thực tế.

## **1. TỔNG QUAN KIẾN TRÚC & LUỒNG XỬ LÝ (ARCHITECTURE & FLOW)**
Hệ thống hoạt động theo cơ chế **Real-time Scoring** kết hợp **Continuous Learning**.
1. **Tracking:** Backend lưu vết thiết bị (`device_sessions`) và hành vi (`user_stats`).
2. **Rule-Based Engine (Tiền xử lý):** Backend lọc Blacklist/Whitelist (IP, User) trước khi gọi AI để tiết kiệm tài nguyên.
3. **Feature Extraction:** Tại thời điểm Checkout, Backend trích xuất dữ liệu Giao dịch, Thiết bị, Người dùng và Vận tốc (Velocity), gửi sang ML Service.
4. **AI Scoring:** ML Service (XGBoost) tính toán rủi ro, kết hợp SHAP trả về `risk_score` và lý do (`top_risk_factors`).
5. **Dynamic Decision:** Backend so sánh `risk_score` với cấu hình động trong DB (`fraud_rule_configs`) để quyết định APPROVE, REVIEW, hoặc DECLINE.
6. **Feedback Loop:** Admin xử lý đơn REVIEW. Sau 15 ngày, dữ liệu "trưởng thành" được đưa vào Retrain.

## **2. THIẾT KẾ CƠ SỞ DỮ LIỆU (DATABASE UPDATES)**

```sql
-- 1. Bảng cấu hình Threshold động (Khắc phục Hardcode)
CREATE TABLE IF NOT EXISTS fraud_rule_configs (
    config_id INT PRIMARY KEY,
    review_threshold DECIMAL(5,2) NOT NULL DEFAULT 40.00,
    decline_threshold DECIMAL(5,2) NOT NULL DEFAULT 75.00,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(user_id)
);
INSERT INTO fraud_rule_configs (config_id, review_threshold, decline_threshold) VALUES (1, 40.00, 75.00);

-- 2. Cập nhật bảng lưu log Fraud để nhận Explainability từ AI
ALTER TABLE fraud_records_and_labels
ADD COLUMN top_risk_factors JSONB NULL; -- Lưu lý do AI nghi ngờ: ["order_total_amount_high", "is_vpn_proxy"]

````

## **3. TỪ ĐIỂN ĐẶC TRƯNG (FEATURE ENGINEERING)**

Bộ dữ liệu đầu vào cho XGBoost (10 Features lõi):

| Feature Name                   | Type | Nguồn / Mapping logic                                    | Xử lý Cold Start (Khách Guest)         |
| :----------------------------- | :--- | :------------------------------------------------------- | :------------------------------------- |
| order\_total\_amount           | Num  | orders.final\_payment\_money                             | Lấy thực tế                            |
| payment\_method                | Cat  | orders.payment\_method (0: COD, 1: VNPAY, 2: MOMO)       | Lấy thực tế                            |
| is\_vpn\_proxy                 | Bin  | device\_sessions.is\_vpn\_proxy                          | Lấy thực tế                            |
| location\_mismatch             | Bin  | So sánh IP Location với địa chỉ giao hàng                | Lấy thực tế                            |
| orders\_per\_device\_24h       | Num  | COUNT(order\_id) nhóm theo device\_fingerprint trong 24h | Tính theo Device, không phụ thuộc User |
| account\_age\_days             | Num  | user\_stats.account\_age\_days                           | **Mặc định = 0**                       |
| reputation\_score              | Num  | user\_stats.reputation\_score                            | **Mặc định = 100**                     |
| failed\_payment\_attempts\_10m | Num  | user\_stats.failed\_payment\_attempts\_10m               | Theo dõi qua Session/Cookie            |
| total\_distinct\_devices       | Num  | user\_stats.total\_distinct\_devices                     | **Mặc định = 1**                       |
| return\_rate                   | Num  | returned\_orders\_count / total\_orders                  | **Mặc định = 0.0**                     |

## **4. ĐẶC TẢ API (ML SERVICE)**

Giao tiếp giữa Backend (Spring Boot) và ML Service (FastAPI). *Không dùng ML để ra quyết định cuối cùng, chỉ tính điểm và giải thích.*

**POST /api/ml/fraud/predict**

``` json
{
  "order_total_amount": 15000000.00,
  "payment_method": 1,
  "is_vpn_proxy": 1,
  "location_mismatch": 1,
  "orders_per_device_24h": 4,
  "account_age_days": 2,
  "reputation_score": 95.0,
  "failed_payment_attempts_10m": 0,
  "total_distinct_devices": 1,
  "return_rate": 0.0
}

```

**RESPONSE BODY**

``` json
{
  "risk_score": 88.50,
  "top_risk_factors": [
    "order_total_amount_high",
    "orders_per_device_spike",
    "vpn_proxy_detected"
  ]
}

```

## **5. FLOW QUYẾT ĐỊNH TẠI BACKEND (DECISION LOGIC)**

Sau khi nhận được `risk_score` từ ML Service, Backend query `fraud_rule_configs` (lưu cache Redis) để ra quyết định:

  - **TH1: risk\_score \<= review\_threshold (VD: \<= 40):**
      - Hệ thống **APPROVE**. Đơn hàng chuyển sang trạng thái CONFIRMED.
  - **TH2: review\_threshold \< risk\_score \<= decline\_threshold (VD: 41 - 75):**
      - Hệ thống **REVIEW**. Đơn chuyển trạng thái PENDING\_REVIEW.
      - Ghi dữ liệu vào `fraud_records_and_labels` kèm mảng `top_risk_factors` từ AI để Admin duyệt.
  - **TH3: risk\_score \> decline\_threshold (VD: \> 75):**
      - Hệ thống **DECLINE**. Chặn thanh toán (Rollback transaction).
      - Đánh cờ `is_restricted = TRUE` trong bảng `user_stats`.

## **6. MLOps & QUẢN LÝ DỮ LIỆU HUẤN LUYỆN**

1.  **Quy tắc Trưởng thành của Nhãn (Label Maturation):**
      - Không lấy ngay các đơn hàng vừa thanh toán thành công vào tập Legit (Nhãn Sạch).
      - Đơn hàng chỉ được tự động gán nhãn 'ecommerce\_marked\_as\_approved' (Y=0) sau khi **Giao hàng thành công \> 15 ngày** (qua thời hạn Chargeback/Return).
2.  **Ground Truth từ Admin:**
      - Khi Admin duyệt đơn thủ công trên Dashboard, nhãn sẽ được gán cứng `admin_confirmed_fraud` (Y=1) hoặc `admin_confirmed_legit` (Y=0).
3.  **Job Huấn luyện (Retraining Job):**
      - Cronjob chạy 1 tuần/lần. Trích xuất toàn bộ dữ liệu có nhãn Y=0 và Y=1 từ database.
      - **Lưu ý SMOTE:** Chỉ áp dụng kỹ thuật cân bằng dữ liệu SMOTE trên tập Train Set. Tuyệt đối không dùng cho Test/Validation Set để tránh Data Leakage làm sai lệch F1-Score.
