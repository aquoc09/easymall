# TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ VÀ KIẾN TRÚC HỆ THỐNG

**Phân hệ:** AI Recommendation & User Behavior Tracking
**Phiên bản:** 2.0 (Chuẩn bị bảo vệ Luận văn)
**Định dạng:** BRD, SRS, Data Pipeline & ML Architecture

## PHẦN 1: TỔNG QUAN KIẾN TRÚC (ARCHITECTURE OVERVIEW)

Hệ thống AI Recommendation được thiết kế theo kiến trúc **Batch-Processing MLOps**, tách biệt hoàn toàn luồng ghi nhận dữ liệu (OLTP) và luồng phân tích (OLAP/ML) để không làm ảnh hưởng đến hiệu năng mua sắm của khách hàng.

### 1.1. Vai trò của các bảng trong AI Pipeline

1. **user_behaviors (Giai đoạn Data Ingestion / Data Collection):** Là "hồ dữ liệu thô" (Raw Data Lake). Nơi ghi nhận mọi "nhất cử nhất động" của người dùng. Đây là nguồn sống của toàn bộ mô hình AI.
2. **product_similarities (Giai đoạn Model Serving - Content Based):** Là bảng Cache kết quả. Lưu trữ độ tương đồng giữa các cặp sản phẩm. Phục vụ tính năng: _"Sản phẩm tương tự"_.
3. **user_recommendations (Giai đoạn Model Serving - Collaborative Filtering):** Là bảng Cache kết quả cá nhân hóa. Phục vụ tính năng: _"Gợi ý dành riêng cho bạn"_ tại Trang chủ.
4. **product_associations (Giai đoạn Model Serving - Market Basket Analysis):** Là bảng Cache kết quả khai phá luật kết hợp. Phục vụ tính năng: _"Thường được mua cùng nhau"_ tại Giỏ hàng.

### 1.2. Luồng dữ liệu End-to-End (Data Flow)

[1] Frontend React → [2] Spring Boot Backend → [3] PostgreSQL (user_behaviors) → [4] Python Data Pipeline (Cleansing & Feature Engineering) → [5] ML Model Training (ALS/LightFM/Apriori) → [6] PostgreSQL Serving Tables → [7] Frontend Hiển thị.

## PHẦN 2: THIẾT KẾ CƠ SỞ DỮ LIỆU & ĐẶC TẢ NGHIỆP VỤ (DATABASE SPECIFICATIONS)

### 2.1. Bảng user_behaviors (Sổ cái Hành vi)

- **Mục tiêu:** Thu thập luồng Clickstream (Implicit Feedback) theo thời gian thực.
- **Actor ghi:** Spring Boot Backend (thông qua @Async hoặc Kafka Message).
- **Bản chất:** Bảng Append-only (Chỉ ghi thêm, không bao giờ UPDATE để tránh MVCC Bloat).

**Phân tích Trường dữ liệu (Data Dictionary):**

| Trường           | Kiểu dữ liệu | Bắt buộc | Vai trò trong AI & Nghiệp vụ                                                           |
| :--------------- | :----------- | :------- | :------------------------------------------------------------------------------------- |
| user_behavior_id | BIGINT       | Có       | Khóa chính vật lý kết hợp created_at (bắt buộc cho Partition).                         |
| user_id          | BIGINT       | Không    | NULL nếu là Guest. Dùng để xây dựng User-Item Matrix cho CF.                           |
| session_id       | VARCHAR      | Có       | Gom nhóm hành vi của Guest (Giải quyết bài toán Cold Start).                           |
| product_id       | BIGINT       | Không    | Sản phẩm tương tác.                                                                    |
| action_type      | VARCHAR      | Có       | Dịch thành **Implicit Score (Trọng số tương tác)**. VD: View=1, Cart=4, Purchase=10.   |
| context_data     | JSONB        | Không    | Dữ liệu siêu linh hoạt: Thiết bị, hệ điều hành, UTM Source. Dùng cho Context-Aware ML. |
| duration_seconds | INT          | Không    | Lọc nhiễu. Nếu View < 3s → Bỏ qua (Click nhầm/Bounce).                                 |
| created_at       | TIMESTAMP    | Có       | Dùng để chia Partition và tính toán **Time Decay** (Hành vi cũ bị giảm trọng số).      |

### 2.2. Bảng product_similarities (Độ tương đồng)

- **Mục tiêu:** Trả lời câu hỏi "Sản phẩm A giống sản phẩm B mức độ nào?".
- **Thời điểm cập nhật:** Chạy Cronjob hàng đêm hoặc hàng tuần.
- **Cột score:** Thể hiện độ tin cậy (Thường từ 0.0 đến 1.0). 0.95 nghĩa là giống nhau 95%.
- **Cột similarity_type:** Có thể là CONTENT_BASED (giống nhau về text, tag, category) hoặc ITEM_CF (những người mua A cũng mua B).

### 2.3. Bảng user_recommendations (Gợi ý Cá nhân)

- **Mục tiêu:** Hiển thị danh sách cá nhân hóa khi User truy cập trang chủ.
- **Cột recommendation_score:** Điểm dự đoán mức độ yêu thích của user với item đó (Predicted Rating). Backend sẽ sắp xếp (ORDER BY) theo điểm này giảm dần.

### 2.4. Bảng product_associations (Gợi ý Mua kèm)

- **Mục tiêu:** Kích thích Upsell/Cross-sell (Ví dụ: Mua Điện thoại gợi ý Ốp lưng).
- **Cột confidence:** Xác suất (0-1). Ví dụ 0.8: Có 80% người mua A sẽ mua B.
- **Cột lift:** Thể hiện "sức mạnh" của luật. Nếu Lift > 1, A và B thực sự có quan hệ thúc đẩy nhau. Lift <= 1 là mua ngẫu nhiên, AI sẽ loại bỏ.

## PHẦN 3: THIẾT KẾ DATA COLLECTION PIPELINE (EVENT TRACKING)

| Tên Event (action_type) | Khi nào phát sinh?                 | Dữ liệu context_data (JSONB) đi kèm           | Trọng số ngầm (Implicit Weight) |
| :---------------------- | :--------------------------------- | :-------------------------------------------- | :------------------------------ |
| VIEW_ITEM               | User ở lại trang chi tiết SP > 3s. | {"referrer": "homepage", "device": "mobile"}  | 1 điểm                          |
| SEARCH                  | Bấm nút Tìm kiếm.                  | {"keyword": "áo thun nam"}                    | 2 điểm                          |
| ADD_TO_CART             | Bấm thêm vào giỏ hàng.             | {"variant_id": 105, "quantity": 2}            | 5 điểm                          |
| ADD_WISHLIST            | Bấm thả tim sản phẩm.              | {"location": "product_page"}                  | 6 điểm                          |
| PURCHASE                | Thanh toán đơn hàng thành công.    | {"order_id": 991, "payment_method": "VNPAY"}  | 10 điểm                         |
| CLICK_REC               | Click vào một SP do AI gợi ý.      | {"ai_model": "hybrid_v1", "rank_position": 2} | Dùng đánh giá CTR của AI        |

## PHẦN 4: THIẾT KẾ AI TRAINING PIPELINE (MACHINE LEARNING)

Quy trình Python ETL kéo dữ liệu từ PostgreSQL và huấn luyện mô hình:

### 4.1. Collaborative Filtering & Hybrid (Tạo user_recommendations)

- **Input:** Bảng user_behaviors (gom nhóm theo user_id và product_id).
- **Feature Engineering:**
  - Tính tổng điểm (Interaction Score) = Σ (Weight × Count).
  - Áp dụng **Time Decay Penalty**: Score = Score × e^(-λt) (Hành vi 1 năm trước gần như = 0 điểm).
- **Thuật toán:** Sử dụng thư viện **LightFM** hoặc **Spark ALS** phân rã ma trận User-Item.
- **Output:** Ghi kết quả Top 50 sản phẩm có điểm dự đoán cao nhất của mỗi User vào bảng user_recommendations.

### 4.2. Content-Based Filtering (Tạo product_similarities)

- **Input:** Bảng products, categories (Lấy Tên SP, Mô tả, Tags).
- **Feature Engineering:** Dùng NLP (TF-IDF Vectorizer hoặc Word2Vec) biến chữ thành Vector đa chiều.
- **Thuật toán:** Tính **Cosine Similarity** giữa các Vector.
- **Output:** Ghi các cặp (Item A, Item B) có Cosine Score > 0.6 vào bảng product_similarities.

### 4.3. Association Rule Mining (Tạo product_associations)

- **Input:** Bảng user_behaviors (Lọc riêng action_type = 'PURCHASE').
- **Thuật toán:** **FP-Growth** hoặc **Apriori**. Tìm các "Rổ hàng" (Basket) thường xuất hiện cùng nhau.
- **Output:** Trích xuất các luật có Confidence > 0.3 và Lift > 1.2, ghi vào bảng product_associations.

## PHẦN 5: TẬP LUẬT NGHIỆP VỤ & LÀM SẠCH DỮ LIỆU (BUSINESS RULES)

1. **Anti-Spam / Bot Filtering:**
   - Nếu một user_id / IP tạo ra > 50 events trong 1 phút → Đánh dấu là Bot, loại bỏ khỏi dữ liệu training.
2. **Session Merging (Quy tắc Khách vãng lai):**
   - Guest (chưa login) xem hàng → Lưu user_id = NULL, session_id = 'ABC'.
   - Khách Login vào tài khoản ID 105.
   - _Không UPDATE bảng behaviors_. Tại tầng Python ETL, script sẽ map session_id = 'ABC' với user_id = 105 để gộp lịch sử, giải quyết triệt để bài toán Cold Start.
3. **Dwell-time Validation:**
   - VIEW_ITEM có duration_seconds < 3 sẽ bị Data Pipeline xóa bỏ vì đây là "Click nhầm", không phản ánh sự yêu thích.

## PHẦN 7: THIẾT KẾ REST API CONTRACT (BACKEND SPRING BOOT)

Hệ thống giao tiếp thông qua các API không đồng bộ và O(1) Cache Reading:

### API 1: Thu thập hành vi (Data Ingestion)

- **Endpoint:** POST /api/v1/tracking/events
- **Nghiệp vụ:** API này phải đáp ứng siêu nhanh (< 20ms). Backend sử dụng Spring @Async hoặc đẩy thẳng vào Kafka/RabbitMQ để worker insert ngầm vào DB, không bắt Client phải chờ.

### API 2: Lấy sản phẩm gợi ý cá nhân hóa (Personalized Recs)

- **Endpoint:** GET /api/v1/recommendations/for-you?userId={id}
- **Nghiệp vụ:** Spring Boot query thẳng vào bảng user_recommendations theo user_id. Tốc độ siêu nhanh do đã có Index và dữ liệu đã được tính sẵn (Pre-computed). Nếu user_id chưa có dữ liệu (Cold Start), Backend tự động Fallback gọi hàm lấy "Top Sản phẩm Bán chạy nhất (Trending)".

### API 3: Lấy sản phẩm tương tự (Related Products)

- **Endpoint:** GET /api/v1/recommendations/products/{id}/similar
- **Nghiệp vụ:** Gọi khi khách vào trang Chi tiết sản phẩm. Trả về list sản phẩm từ bảng product_similarities.
