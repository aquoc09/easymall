# **ĐẶC TẢ KIẾN TRÚC HỆ THỐNG AI PHÁT HIỆN GIAN LẬN (FRAUD DETECTION)**

**Dự án:** EasyMall E-commerce Platform **Phiên bản:** 3.0 (Production Ready & Enterprise Architecture) **Phân hệ:** AI / Machine Learning & Fraud Prevention

## **1. Executive Summary (Tóm tắt mục tiêu)**

Mục tiêu của module Fraud Detection AI là phát hiện, đánh giá và ngăn chặn các hành vi gian lận trong thời gian thực (Real-time) xuyên suốt quá trình mua sắm, thanh toán và sử dụng khuyến mãi trên nền tảng EasyMall.

**Các nền tảng công nghệ cốt lục được áp dụng:**

- Real-time Fraud Scoring
- Rule-based Fraud Detection (Tiền xử lý)
- Machine Learning Fraud Detection (XGBoost)
- Continuous Learning (Học liên tục)
- Explainable AI (SHAP - Trí tuệ nhân tạo có thể giải thích)
- Graph Fraud Detection (Phát hiện gian lận qua mạng lưới)

**Mục tiêu Kinh doanh (Business Objectives):**

- Giảm thiểu gian lận thanh toán (Chargeback Fraud).
- Giảm thất thoát ngân sách khuyến mãi (Promotion/Coupon Abuse).
- Giảm thiểu tình trạng chiếm đoạt tài khoản (Account Takeover).
- Giảm chi phí vận hành cho việc duyệt đơn thủ công (Manual Review Cost).
- Tối đa hóa tỷ lệ phát hiện gian lận với độ chính xác cao.

## **2. Kiến trúc Tổng thể (System Architecture)**

### **2.1. Thành phần Hệ thống (System Components)**

Luồng kiến trúc được thiết kế tách biệt giữa luồng xử lý giao dịch thời gian thực (Online) và luồng huấn luyện mô hình (Offline).

**Luồng Real-time (Online Pipeline):**

1. Frontend Client
2. Spring Boot Backend (Core API)
3. Fraud Rule Engine (Bộ lọc tĩnh)
4. ML Service (FastAPI + XGBoost Engine)
5. Fraud Decision Engine (Hệ thống ra quyết định)
6. Transaction Database

**Luồng Huấn luyện (Offline Pipeline):**

1. Transaction Database
2. Feature Store (Nơi lưu trữ đặc trưng)
3. Training Pipeline (Huấn luyện định kỳ)
4. Model Registry (Quản lý phiên bản)
5. Canary Deployment (Triển khai nhúng)
6. Production Model

### **2.2. Luồng Xử lý Thời gian thực (Real-time Flow)**

- **Bước 1:** Người dùng thực hiện thao tác tạo đơn hàng (Checkout).
- **Bước 2:** Backend thu thập toàn bộ Context bao gồm: User Features, Device Features, Transaction Features và Promotion Features.
- **Bước 3 (Rule Engine):** Chạy bộ lọc tĩnh trước. Nếu vi phạm (thuộc Blacklisted Device, IP, User) -&gt; **DECLINE** ngay lập tức, không gọi sang AI để tiết kiệm tài nguyên.
- **Bước 4 (ML Scoring):** Nếu vượt qua Rule Engine, Backend gọi API POST /api/ml/fraud/predict.
- **Bước 5 (Explainable AI):** ML Service trả về điểm rủi ro (risk_score), lý do nghi ngờ (top_risk_factors), và phiên bản mô hình (model_version).
- **Bước 6 (Decision):** Backend đọc cấu hình ngưỡng động từ fraud_rule_configs để đối chiếu điểm số và ra quyết định (APPROVE / REVIEW / DECLINE).
- **Bước 7 (Logging):** Toàn bộ kết quả và lý do được lưu vào bảng fraud_records_and_labels để phục vụ Audit và Retrain.

## **3. Kịch bản Gian lận (Fraud Scenarios)**

- **Payment Fraud (Gian lận thanh toán):** Sử dụng thẻ tín dụng đánh cắp. Dấu hiệu nhận biết: failed_payment_attempts_10m tăng cao, giá trị đơn hàng lớn bất thường, sử dụng VPN/Proxy liên tục thất bại.
- **Account Takeover (Chiếm đoạt tài khoản):** Kẻ gian đăng nhập trái phép. Dấu hiệu nhận biết: location_mismatch (khác biệt địa lý), total_distinct_devices tăng đột biến, xuất hiện IP/Thiết bị mới chưa từng tồn tại trong lịch sử User.
- **Coupon Abuse (Lạm dụng mã giảm giá):** Dấu hiệu nhận biết: Nhiều tài khoản khác nhau nhưng cùng chung thiết bị (Device Fingerprint), cùng chung địa chỉ giao hàng cố sử dụng một loại Coupon.
- **Promotion Abuse (Trục lợi khuyến mãi tân thủ):** Dấu hiệu nhận biết: Tạo hàng loạt tài khoản mới (account_age_days = 0), dùng voucher lần đầu trên cùng một mạng hoặc thiết bị.
- **Fraud Ring Detection (Đường dây gian lận):** Nhiều tài khoản liên kết chặt chẽ với nhau (cùng Device, IP, Phone, Address) tạo thành một mạng lưới (Fraud Network) trục lợi có tổ chức.

## **4. Trích xuất Đặc trưng (Feature Engineering)**

### **4.1. Nhóm Đặc trưng Dữ liệu (Core Features)**

| Nhóm            | Tên Feature                 | Nguồn Dữ liệu (Source) | Xử lý Cold Start (Guest) |
| --------------- | --------------------------- | ---------------------- | ------------------------ |
| **Device**      | is_vpn_proxy                | device_sessions        | Lấy thực tế              |
| **Device**      | device_fingerprint_match    | device_sessions        | Lấy thực tế              |
| **Device**      | location_mismatch           | device_sessions        | Lấy thực tế              |
| **Device**      | total_distinct_devices      | user_stats             | Lấy thực tế              |
| **User**        | account_age_days            | user_stats             | Mặc định = 0             |
| **User**        | reputation_score            | user_stats             | Mặc định = 100           |
| **User**        | return_rate                 | user_stats             | Mặc định = 0             |
| **User**        | failed_payment_attempts_10m | user_stats             | Theo Session             |
| **Transaction** | order_total_amount          | orders                 | Lấy thực tế              |
| **Transaction** | payment_method              | orders                 | Lấy thực tế              |
| **Transaction** | shipping_method             | orders                 | Lấy thực tế              |
| **Transaction** | total_items                 | order_details          | Lấy thực tế              |

### **4.2. Nhóm Đặc trưng Nâng cao (Advanced Features)**

| Loại Đặc trưng         | Tên Feature              | Logic Nghiệp vụ / Ý nghĩa                                     |
| ---------------------- | ------------------------ | ------------------------------------------------------------- |
| **Velocity (Vận tốc)** | orders_per_device_24h    | Device Velocity: Số lượng đơn trên cùng 1 thiết bị trong 24h. |
| **Velocity (Vận tốc)** | orders_per_ip_24h        | IP Velocity: Số lượng đơn trên cùng 1 IP trong 24h.           |
| **Velocity (Vận tốc)** | coupon_usage_24h         | Promotion Velocity: Số lần dùng coupon trên toàn hệ thống/IP. |
| **Graph (Mạng lưới)**  | shared_device_count      | Số lượng Account khác nhau dùng chung thiết bị.               |
| **Graph (Mạng lưới)**  | shared_ip_count          | Số lượng Account khác nhau dùng chung IP.                     |
| **Graph (Mạng lưới)**  | shared_coupon_count      | Số lượng Account khác nhau cùng dùng 1 loại coupon dị thường. |
| **Graph (Mạng lưới)**  | suspicious_network_score | Điểm số tổng hợp nghi ngờ mạng lưới có tổ chức.               |

## **5. Chiến lược Nhãn Dữ liệu (Labeling Strategy)**

**Định nghĩa Nhãn (Labels):**

- **Gian lận (Fraud = 1):** Khi trạng thái là ecommerce_chargeback_fraud hoặc admin_confirmed_fraud.
- **Hợp lệ (Legit = 0):** Khi trạng thái là admin_confirmed_legit hoặc system_marked_legit.

**Quy tắc Trưởng thành (Label Maturation)**:Để ngăn chặn hiện tượng rò rỉ nhãn sai (Label Leakage), một đơn hàng CHỈ ĐƯỢC hệ thống tự động gán nhãn Legit (0) khi hội đủ 3 điều kiện:

1. Trạng thái đơn hàng là **DELIVERED**.
2. Thời gian tính từ lúc giao thành công đã **quá 15 ngày**.
3. Không phát sinh bất kỳ khiếu nại (Dispute/Chargeback) nào.

## **6. Luồng Huấn luyện (Training Pipeline)**

- **Nguồn dữ liệu (Data Sources):** users, orders, order_details, device_sessions, coupon_usages, user_stats, fraud_records_and_labels.
- **Chiến lược Tách tập (Dataset Split):**
  - Train Set: 70%
  - Validation Set: 15%
  - Test Set: 15%
  - _Kỹ thuật bắt buộc:_ Stratified Split & Time-based Split.
- **Xử lý Mất cân bằng (Imbalanced Data):** Kỹ thuật SMOTE hoặc cấu hình scale_pos_weight. **Lưu ý:** Chỉ áp dụng các kỹ thuật này trên tập Training Set.
- **Thuật toán (Algorithms):**
  - _Primary:_ XGBoost
  - _Future Integration:_ LightGBM, Random Forest, Isolation Forest, Graph Neural Network.

## **7. Tiêu chí Đánh giá Mô hình (Model Evaluation Criteria)**

### **7.1. Chỉ số Kỹ thuật (Technical Metrics)**

| Metric                      | Target (Mục tiêu) |
| --------------------------- | ----------------- |
| **Recall** (Rất quan trọng) | &gt;= 85%         |
| **Precision**               | &gt;= 60%         |
| **F1-Score**                | &gt;= 70%         |
| **ROC-AUC**                 | &gt;= 0.90        |
| **PR-AUC**                  | &gt;= 0.75        |

### **7.2. Chỉ số Kinh doanh (Business KPIs)**

| KPI                      | Target (Mục tiêu)       |
| ------------------------ | ----------------------- |
| **Fraud Detection Rate** | &gt;= 85%               |
| **False Positive Rate**  | &lt;= 5%                |
| **Manual Review Rate**   | &lt;= 10% tổng đơn hàng |
| **Chargeback Reduction** | &gt;= 50%               |

## **8. AI Có Thể Giải Thích (Explainable AI - SHAP)**

Sử dụng **SHAP Tree Explainer** để giải mã hộp đen AI, cung cấp cho Admin nguyên nhân gốc rễ.

_Output JSON ví dụ:_

```json
{
  "risk_score": 88.5,
  "top_risk_factors": [
    "order_total_amount (high_variance)",
    "location_mismatch (true)",
    "orders_per_device_24h (velocity_spike)"
  ],
  "model_version": "v1.2.0"
}
```

**9. Thiết kế Lưu trữ Đặc trưng (Feature Store Design)**

| Loại Feature Store        | Môi trường phục vụ                                       | Nơi lưu trữ (Storage)                | Vòng đời (TTL)                        |
| ------------------------- | -------------------------------------------------------- | ------------------------------------ | ------------------------------------- |
| **Offline Feature Store** | Huấn luyện (Training), Validation, Phân tích (Analytics) | PostgreSQL Analytics, Data Warehouse | Vĩnh viễn (Historical)                |
| **Online Feature Store**  | Chấm điểm thời gian thực (Real-time Scoring)             | Redis                                | 24 giờ, 7 ngày, 30 ngày (Tùy feature) |

**10. API Phục vụ Thời gian thực (Real-time Prediction API)**\
\
**Endpoint:** POST /api/ml/fraud/predict

- **Request Payload:** Chứa Device Features, User Features, Transaction Features.
- **Response Payload:** Trả về risk_score, mảng top_risk_factors, và model_version.

**11. Hệ thống Ra Quyết Định (Decision Engine)**\
\
Ngưỡng rủi ro (Threshold) được lưu tại bảng fraud_rule_configs. Backend là nơi đưa ra quyết định cuối cùng dựa trên luật động:

| Mức độ Điểm (Score) | Hành động (Action)                           |
| ------------------- | -------------------------------------------- |
| **0 - 40**          | **APPROVE** (Duyệt tự động)                  |
| **41 - 75**         | **REVIEW** (Tạm giữ chờ Admin kiểm tra)      |
| **&gt; 75**         | **DECLINE** (Từ chối giao dịch ngay lập tức) |

**12. Quản trị và Triển khai Mô hình (Model Registry & Governance)**\
\
**Bảng Quản lý:** ml_model_registry

- **Dữ liệu lưu trữ:** model_version, dataset_version, feature_set_version, các chỉ số đánh giá (precision, recall, f1, auc).
- **Trạng thái Vòng đời (Lifecycle Status):** TRAINING -&gt; VALIDATED -&gt; STAGING -&gt; CANARY -&gt; PRODUCTION -&gt; RETIRED.

**Chiến lược Canary Deployment (Triển khai nhúng an toàn):**

- **Stage 1:** 5% traffic.
- **Stage 2:** 20% traffic.
- **Stage 3:** 50% traffic.
- **Stage 4:** 100% traffic.
- *Điều kiện Rollback:* Nếu Precision giảm &gt;10%, Recall giảm &gt;5%, hoặc Error Rate API &gt;1%.

**13. Giám sát Độ lệch Dữ liệu (Data Drift Monitoring)**\
\
Theo dõi liên tục **Feature Drift** và **Prediction Drift** để phát hiện dấu hiệu mô hình bị "lỗi thời".

- **Metrics sử dụng:** PSI (Population Stability Index), KS Statistic, Jensen-Shannon.
- **Alerting:** Khi PSI &gt; 0.25 -&gt; Tự động gửi cảnh báo qua Slack/Email và tạo Ticket yêu cầu Retraining.

**14. Kiểm toán & Tuân thủ (Audit & Compliance)**\
\
Bảng fraud_records_and_labels lưu vết mọi quyết định AI trong tối thiểu **5 năm** để phục vụ Audit, Chargeback Investigation và Fraud Investigation.\
Các trường lưu trữ bắt buộc: prediction_time, model_version, feature_set_version, risk_score, top_risk_factors, reviewer_id, final_label.**15. Chiến lược Huấn luyện lại (Retraining Strategy)**

- **Daily (Hàng ngày):** Job Data Extraction gom dữ liệu sạch.
- **Weekly (Hàng tuần):** Tiến hành Retraining mô hình trên tập dữ liệu mới.
- **Monthly (Hàng tháng):** Full Evaluation để đánh giá chéo hiệu năng tổng thể.
- **Trigger khẩn cấp:** Kích hoạt Retrain ngay khi PSI &gt; 0.25, Recall giảm &gt; 5%, hoặc phát hiện Pattern gian lận mới.

**16. Thay đổi Cơ sở dữ liệu (Database Changes)**

- **Bảng mới:** fraud_rule_configs, feature_registry, ml_model_registry.
- **Cột mới bổ sung:** fraud_records_and\_[labels.top](http://labels.top)\_risk_factors, fraud_records_and_labels.model_version, fraud_records_and_labels.feature_set_version.

**17. Lộ trình Triển khai (Roadmap)**

- **Phase 1:** Triển khai Rule Engine kết hợp mô hình XGBoost cơ bản.
- **Phase 2:** Tích hợp Explainability với SHAP.
- **Phase 3:** Xây dựng Offline & Online Feature Store (Redis).
- **Phase 4:** Tích hợp Graph Fraud Detection (Mạng lưới gian lận).
- **Phase 5:** Tự động hóa MLOps (CI/CD/CT Pipeline).
- **Phase 6:** Nghiên cứu và ứng dụng Graph Neural Network (GNN).

```

Tôi đã chuyển đổi toàn bộ tài liệu sang định dạng Markdown theo yêu cầu của bạn. Bạn có muốn tôi thực hiện bất kỳ điều chỉnh nào khác cho tài liệu này không?
``
```
