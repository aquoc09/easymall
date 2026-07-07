# TECHNICAL DESIGN DOCUMENT: USER BEHAVIOR DATA PIPELINE & AI RECOMMENDATION
**Project**: EasyMall - Fashion E-commerce Recommendation System
**Role**: Principal Data Engineer + Senior MLOps Architect + Senior Backend Architect + Senior Business Analyst

---

## 1. Phân tích sự khác biệt (Gap Analysis: Tài liệu cũ vs Database hiện tại)

Dựa trên việc kiểm tra trực tiếp mã nguồn và cấu trúc Entity mới nhất của hệ thống, đã có nhiều sự thay đổi giữa bản thiết kế cũ và Database hiện tại. Cụ thể:

### 1.1. Những nội dung đã lỗi thời trong tài liệu cũ
* **Tên cột và cấu trúc bảng `user_behaviors`:**
  * Tài liệu cũ sử dụng `trace_id`, `event_type`, `device_info`, và `user_id` kiểu `UUID`.
  * **Thực tế Database mới:** Sử dụng PK là `user_behavior_id` (BIGINT), `user_id` kiểu `BIGINT`, sử dụng `action_type` thay cho `event_type`. Đã loại bỏ `device_info` và `trace_id` khỏi bảng này. Đã bổ sung thêm các cột quan trọng phục vụ tìm kiếm và phân loại: `keyword` (cho action SEARCH) và `category_id`.
* **Quản lý Session:**
  * Tài liệu cũ thiết kế bảng `behavior_sessions`.
  * **Thực tế Database mới:** Sử dụng bảng `device_sessions` với các thuộc tính mạnh mẽ hơn cho việc fingerprinting và fraud detection (`ip_address`, `user_agent`, `device_fingerprint`, `is_vpn_proxy`, `location_country`).
* **Các bảng bổ trợ Features (Mới xuất hiện):**
  * Database hiện tại có bảng `user_stats` chứa các features cực kỳ giá trị cho AI (ví dụ: `reputation_score`, `total_orders`, `returned_orders_count`, `account_age_days`).
  * Bảng `products` đã được denormalize để chứa các chỉ số pre-aggregated: `view_count`, `sold_count`, `rating_avg`, `popularity_score`. Điều này thay đổi hoàn toàn cách chúng ta xây dựng Fallback Model (Cold-start).

---

## 2. Kiến trúc tổng thể (Overall Architecture)

Kiến trúc tổng thể của User Behavior Data Pipeline được thiết kế để thu thập Clickstream từ Frontend, xử lý Ingestion ở Backend, lưu trữ tại Database và chuyển đổi (ETL) thành dữ liệu đầu vào cho AI Recommendation System.

```mermaid
graph TD
    %% Frontend Layer
    subgraph Frontend [1. Frontend Layer - React]
        A1[Web/Mobile App]
        A2[Event Tracker SDK]
        A1 -->|Trigger Events| A2
    end

    %% API Gateway & Backend Layer
    subgraph Backend [2. Backend Ingestion Layer - Spring Boot]
        B1[API Gateway / Controller]
        B2[Ingestion Service]
        B3[Async Event Publisher]
        B4[Thread Pool / Executor]
        
        A2 -->|POST /api/v1/behaviors| B1
        B1 --> B2
        B2 --> B3
        B3 -->|@Async| B4
    end

    %% Database Layer
    subgraph Storage [3. Storage Layer - PostgreSQL]
        C1[(user_behaviors)]
        C2[(device_sessions)]
        C3[(user_stats & products)]
        
        B4 -->|JDBC Batch Insert| C1
        B4 -->|Insert if not exists| C2
    end

    %% ETL Layer
    subgraph ETL [4. Batch ETL Layer - Python / Spark]
        D1[Scheduler - Airflow/Cron]
        D2[Data Extractor]
        D3[Data Cleanser & Anti-Fraud]
        D4[Feature Engineer & Time Decay]
        D5[Interaction Matrix Generator]
        
        D1 -->|Trigger| D2
        D2 -->|Select| C1
        D2 -->|Join| C3
        D2 --> D3
        D3 --> D4
        D4 --> D5
    end

    %% AI Layer
    subgraph AI [5. AI & Recommendation Layer - LightFM]
        E1[ALS / LightFM Model]
        E2[Training Pipeline]
        E3[FastAPI Serving]
        
        D5 -->|Pickle / Parquet| E2
        E2 --> E1
        E1 --> E3
    end
    
    %% Loop back
    E3 -->|Get Recommendations| B1
```

---

## 3. Frontend Event Tracking Design

Event Tracking SDK được thiết kế chạy ngầm, không block main thread. Các hành vi người dùng được phân loại thông qua trường `action_type`.

### 3.1. Danh sách Events (Action Types)

| Action Type (`action_type`) | Trigger Component | Mô tả & Context Data đi kèm |
| :--- | :--- | :--- |
| `VIEW_PRODUCT` | `ProductDetail` | Kích hoạt khi user mở trang chi tiết sản phẩm. Cần truyền `product_id`, `category_id`. |
| `SEARCH_PRODUCT` | `SearchBar` | Kích hoạt khi user gõ phím (Debounce) hoặc enter. Cần truyền chuỗi tìm kiếm vào cột `keyword`. |
| `ADD_TO_CART` | `ProductCard`, `Detail` | Kích hoạt khi thêm vào giỏ. Cần truyền `product_id`, `variant_id`. |
| `WISHLIST_TOGGLE` | `ProductCard`, `Detail` | Kích hoạt khi thả tim. Tín hiệu Implicit Feedback mạnh. |
| `PURCHASE_COMPLETE`| `CheckoutPage` | Kích hoạt sau khi gọi API tạo Order thành công. |
| `CATEGORY_VIEW` | `CategoryPage` | Kích hoạt khi load trang danh mục. Truyền `category_id`. |

### 3.2. JSON Payload Format

```json
{
  "user_id": 10502, 
  "session_id": "sess_938jf2_881",
  "action_type": "ADD_TO_CART",
  "product_id": 88231,
  "variant_id": 9921,
  "category_id": 12,
  "keyword": null,
  "context_data": {
    "source_page": "home_recommendation",
    "position_clicked": 3,
    "price_at_action": 250000
  }
}
```

---

## 4. PostgreSQL Database Design (Đồng bộ với hệ thống hiện hành)

Dựa trên thực tế mã nguồn Entity (`UserBehaviorEntity`, `DeviceSessionEntity`, `UserStatsEntity`), thiết kế Schema của Data Pipeline như sau:

### 4.1. DDL Script for User Behaviors & Devices

```sql
-- 1. Table for Device/User Sessions (Identity & Anti-fraud)
CREATE TABLE device_sessions (
    device_session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500) NOT NULL,
    device_fingerprint VARCHAR(255),
    location_country VARCHAR(100),
    is_vpn_proxy BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table for User Behaviors (Clickstream)
CREATE TABLE user_behaviors (
    user_behavior_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(100) NOT NULL,
    product_id BIGINT REFERENCES products(product_id) ON DELETE SET NULL,
    category_id BIGINT REFERENCES categories(category_id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL,
    keyword VARCHAR(255),
    context_data JSONB,
    variant_id BIGINT REFERENCES product_variants(variant_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index tối ưu cho truy vấn ETL và Analytics
CREATE INDEX idx_behavior_user_time ON user_behaviors(user_id, created_at DESC);
CREATE INDEX idx_behavior_product ON user_behaviors(product_id, action_type);
CREATE INDEX idx_behavior_session ON user_behaviors(session_id);
```

### 4.2. Khai thác dữ liệu Master & Stats cho AI
Ngoài log hành vi, AI Pipeline sẽ lấy thêm dữ liệu từ:
*   **`user_stats`**: Lấy `reputation_score`, `total_orders`, `returned_orders_count` để phân loại chất lượng người dùng (Phạt trọng số đối với user có tỷ lệ return cao hoặc điểm uy tín thấp).
*   **`products`**: Cột `popularity_score` và `rating_avg` sẽ được dùng làm **Fallback Recommendation** cho Cold-Start Users (Người dùng mới chưa có click nào).

---

## 5. Đề xuất cải tiến hệ thống (System & Schema Improvements)

Mặc dù cấu trúc DB hiện tại đã đáp ứng tốt việc lưu trữ, nhưng để xây dựng một AI Recommendation hạng nặng (như Shopee/Amazon), tôi đề xuất các nâng cấp sau:

### 5.1. Bổ sung cấu trúc Database
1.  **Thêm cột `trace_id` (VARCHAR) vào `user_behaviors`**:
    *   *Lý do:* Hệ thống hiện tại thiếu cơ chế Deduplication (Idempotency Key). Nếu mạng lag, App gửi lại 1 event `ADD_TO_CART` 3 lần, DB sẽ lưu 3 dòng, khiến thuật toán AI hiểu lầm user cực kỳ thích sản phẩm này. Thêm `trace_id` + Unique Index sẽ chặn đứng spam click ở tầng DB.
2.  **Thêm cột `duration_seconds` (INT) vào `user_behaviors`**:
    *   *Lý do:* Xem 1 sản phẩm 3 giây (Vô tình click rồi back lại ngay - Bounce rate) hoàn toàn khác với việc xem 3 phút (Thực sự đọc description). Dữ liệu *Dwell Time* là "mỏ vàng" để train AI.
3.  **Thay đổi cơ chế Partitioning JPA**:
    *   *Lý do:* `UserBehaviorEntity` hiện đang tạo chung vào một bảng. Với E-commerce, log này có thể đạt hàng triệu records/ngày. Đề xuất sử dụng Native SQL Partitioning by Range (`created_at`) để tách bảng theo tháng (ví dụ `user_behaviors_2026_07`), giúp ETL quét dữ liệu nhanh hơn gấp hàng chục lần và dễ dàng xoá log cũ.

### 5.2. Cải tiến Luồng Behavior Events
1.  **Ghi nhận sự kiện `IMPRESSION` (Lượt hiển thị)**:
    *   *Lý do:* Thuật toán AI rất cần biết "Sản phẩm nào đã được show ra nhưng user KHÔNG click". Việc chỉ lưu `VIEW_PRODUCT` dẫn đến thiên lệch (Bias) dữ liệu tích cực. Ta cần bắt event khi các `ProductCard` xuất hiện trong viewport (sử dụng `IntersectionObserver` ở React).

---

## 6. Feature Engineering & Trọng số hành vi (Action Weight)

Các hành vi có giá trị khác nhau đối với mô hình gợi ý. Mua hàng thể hiện sự quan tâm mãnh liệt hơn so với chỉ lướt xem.

**Bảng trọng số cơ sở (Base Weight - Cập nhật theo Schema mới):**
| Action Type (`action_type`) | Base Weight | Phân tích |
| :--- | :--- | :--- |
| `CATEGORY_VIEW` | 0.5 | Quan tâm rất chung chung. |
| `VIEW_PRODUCT` | 1.0 | Quan tâm cơ bản. |
| `SEARCH_PRODUCT`| 2.0 | Tương tác có chủ đích rõ ràng (kết hợp `keyword`). |
| `WISHLIST_TOGGLE`| 3.0 | Tín hiệu cực tốt, lưu để mua sau. |
| `ADD_TO_CART` | 4.0 | Rất gần với quyết định chuyển đổi (Conversion). |
| `PURCHASE_COMPLETE`| 10.0 | Tín hiệu mạnh nhất. User đã thanh toán. |

### Xử lý Time Decay (Phai nhòa theo thời gian)
Hành vi mua áo mùa đông cách đây 6 tháng không còn giá trị trong mùa hè hiện tại. Sử dụng **Exponential Decay**:
$$ W_{final} = W_{base} \times e^{-\lambda \times \Delta t} $$
Trong đó $\Delta t$ là số ngày trôi qua từ `created_at` đến hiện tại.

---

## 7. Python Batch ETL Pipeline (Cập nhật)

Pipeline này chạy định kỳ hàng ngày, biến hàng triệu record logs thành `Interaction Matrix` gọn nhẹ. File `ai/etl/behavior_pipeline.py`.

```python
import pandas as pd
import numpy as np
from sqlalchemy import create_engine
import datetime

# 1. Connect to PostgreSQL
DB_URI = "postgresql://user:pass@localhost:5432/easymall"
engine = create_engine(DB_URI)

# 2. Extract Data (Lấy 90 ngày gần nhất + Join thông tin uy tín của user)
query = """
    SELECT ub.user_id, ub.product_id, ub.action_type, ub.created_at, ub.session_id,
           us.reputation_score, ds.is_vpn_proxy
    FROM user_behaviors ub
    LEFT JOIN user_stats us ON ub.user_id = us.user_id
    LEFT JOIN device_sessions ds ON ub.session_id = ds.session_id
    WHERE ub.created_at >= CURRENT_DATE - INTERVAL '90 days'
      AND ub.user_id IS NOT NULL AND ub.product_id IS NOT NULL
"""
df = pd.read_sql(query, engine)

# 3. Data Cleansing & Fraud Filtering
# Loại bỏ các hành vi từ Bot hoặc User có điểm uy tín < 30, hoặc dùng VPN
df = df[(df['reputation_score'] >= 30) | (df['reputation_score'].isna())]
df = df[df['is_vpn_proxy'] != True]

# Bỏ qua các session click spam (vd > 300 action/session)
session_counts = df.groupby('session_id').size()
bot_sessions = session_counts[session_counts > 300].index
df = df[~df['session_id'].isin(bot_sessions)]

# 4. Map Action Weights
weight_map = {
    'VIEW_PRODUCT': 1.0,
    'SEARCH_PRODUCT': 2.0,
    'WISHLIST_TOGGLE': 3.0,
    'ADD_TO_CART': 4.0,
    'PURCHASE_COMPLETE': 10.0
}
df['base_weight'] = df['action_type'].map(weight_map).fillna(0)

# 5. Time Decay Calculation
current_time = pd.Timestamp.now(tz='UTC')
df['created_at'] = pd.to_datetime(df['created_at'], utc=True)
df['days_ago'] = (current_time - df['created_at']).dt.total_seconds() / (24 * 3600)

half_life_days = 15
decay_constant = np.log(2) / half_life_days
df['decayed_weight'] = df['base_weight'] * np.exp(-decay_constant * df['days_ago'])

# 6. Interaction Matrix Aggregation
interaction_df = df.groupby(['user_id', 'product_id'])['decayed_weight'].sum().reset_index()
interaction_df.rename(columns={'decayed_weight': 'interaction_score'}, inplace=True)

# 7. Normalize Scores (Log1p để làm mượt outlier click)
interaction_df['interaction_score'] = np.log1p(interaction_df['interaction_score'])

# 8. Output to Parquet cho Model LightFM
output_path = f"dataset/interaction_matrix_{datetime.date.today().strftime('%Y%m%d')}.parquet"
interaction_df.to_parquet(output_path, index=False)
```

---

## 8. Best Practices & Production Guidelines

1. **Async Ingestion**: API tracking (`POST /api/v1/behaviors`) bắt buộc phải dùng cơ chế ThreadPool `@Async` hoặc đẩy Message vào Apache Kafka/RabbitMQ. Phản hồi API luôn là `202 Accepted` < 10ms để không block UI của người dùng.
2. **Cold-Start Fallback**: Hệ thống AI luôn có độ trễ (Batch processing). Đối với user hoàn toàn mới, Frontend/Backend phải fallback ngay lập tức sang thuật toán "Sản phẩm phổ biến nhất" (Đọc trực tiếp từ cột `popularity_score` trong bảng `products` hiện tại).
3. **Data Security**: JWT Tokens phải được validate ở API Gateway. ID của người dùng không lấy từ Payload của Request mà phải extract từ claims của JWT đã được xác thực, ngăn chặn giả mạo log.
4. **ETL Idempotency**: Job xử lý Data bằng Python luôn phải đảm bảo tính lũy đẳng (chạy lỗi chạy lại bao nhiêu lần thì kết quả Parquet xuất ra vẫn không thay đổi).
5. **GDPR/Privacy**: Luôn cung cấp API cho phép user "Xóa lịch sử duyệt web" (Xóa record trong bảng `user_behaviors` với `user_id` tương ứng).
