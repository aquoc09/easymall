# EasyMall — Backend API

> **Đồ án tốt nghiệp (DATN)** — Nền tảng thương mại điện tử thời trang xây dựng trên Spring Boot 3.

---

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tech Stack](#tech-stack)
- [Kiến trúc dự án](#kiến-trúc-dự-án)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt & Chạy local](#cài-đặt--chạy-local)
- [Biến môi trường](#biến-môi-trường)
- [Database Migration](#database-migration)
- [API Overview](#api-overview)
- [Cấu trúc module](#cấu-trúc-module)

---

## Giới thiệu

**EasyMall** là một RESTful API backend cho hệ thống e-commerce chuyên về thời trang. Dự án hỗ trợ đầy đủ vòng đời của một sản phẩm từ quản lý danh mục, sản phẩm, biến thể (size/màu), tồn kho đến quản lý người dùng và phân quyền theo vai trò (RBAC).

### Tính năng chính

- 🔐 **Authentication** — JWT (Access + Refresh Token), đăng ký/đăng nhập, đổi mật khẩu, xác thực email
- 👥 **RBAC** — Phân quyền chi tiết theo Role & Permission
- 📦 **Product Module** — CRUD sản phẩm, quản lý biến thể (SKU), ảnh sản phẩm, tồn kho
- 🗂️ **Category** — Danh mục đa cấp với slug, hình ảnh
- 🔍 **Full-text Search** — PostgreSQL `tsvector` với `unaccent` hỗ trợ tiếng Việt
- 📧 **Email** — Gửi mail qua SMTP (Spring Mail)
- 💳 **VNPay** — Tích hợp cổng thanh toán nội địa *(đang phát triển)*
- ☁️ **AWS S3** — Lưu trữ ảnh sản phẩm

---

## Tech Stack

| Layer | Công nghệ |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.2 |
| Security | Spring Security + OAuth2 Resource Server (JWT) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15+ |
| Cache / Session | Redis (Spring Session) |
| Search | Elasticsearch + PostgreSQL Full-text Search |
| Migration | Flyway |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Build | Maven (Maven Wrapper included) |
| Storage | AWS S3 |
| Email | Spring Mail (Gmail SMTP) |

---

## Kiến trúc dự án

Dự án áp dụng **Package-by-Layer** với 3-tier architecture:

```
src/main/java/com/quocnva/easymall/
├── config/                  # SecurityConfig, CORS, Bean configs
├── controller/              # REST endpoints (không chứa business logic)
│   ├── AuthController
│   ├── UserController
│   ├── CategoryController
│   ├── ProductController
│   ├── RoleController
│   ├── PermissionController
│   └── AddressController
├── service/                 # Interface định nghĩa business logic
│   └── impl/                # Triển khai service
├── repository/              # Spring Data JPA repositories
├── entity/                  # JPA Entities (mapping DB tables)
│   ├── UserEntity
│   ├── ProductEntity
│   ├── ProductVariantEntity
│   ├── ProductImageEntity
│   ├── InventoryTransactionEntity
│   ├── CategoryEntity
│   └── ...
├── dtos/
│   ├── request/             # DTOs nhận từ client
│   └── response/            # DTOs trả về client
├── mapper/                  # MapStruct mappers (Entity ↔ DTO)
├── exception/               # Custom exceptions + @RestControllerAdvice
├── enums/                   # Enumerations
└── util/                    # Tiện ích (SlugUtil, JwtUtil...)
```

---

## Yêu cầu môi trường

| Công cụ | Phiên bản tối thiểu |
|---|---|
| JDK | 21 |
| PostgreSQL | 15+ |
| Redis | 7+ |
| Maven | 3.9+ (hoặc dùng `mvnw` đính kèm) |

> **Docker:** Nếu bạn có Docker, Spring Boot sẽ tự khởi động PostgreSQL & Redis qua `spring-boot-docker-compose` (xem `compose.yaml` nếu có).

---

## Cài đặt & Chạy local

### 1. Clone repository

```bash
git clone <repository-url>
cd easymall
```

### 2. Tạo database PostgreSQL

```sql
CREATE DATABASE easymall;
```

Kích hoạt extension cần thiết (Flyway sẽ tự chạy, nhưng nếu cần thủ công):

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;
```

### 3. Tạo file biến môi trường

Tạo file `.env` hoặc set environment variables (xem [Biến môi trường](#biến-môi-trường)).

### 4. Chạy ứng dụng

**Với Maven Wrapper (khuyến nghị):**

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

**Hoặc build JAR rồi chạy:**

```bash
.\mvnw.cmd clean package -DskipTests
java -jar target/easymall-0.0.1-SNAPSHOT.jar
```

### 5. Kiểm tra ứng dụng

Sau khi khởi động thành công, API sẽ sẵn sàng tại:

```
http://localhost:8080/easymall
```

---

## Biến môi trường

Tất cả cấu hình nhạy cảm được đọc qua biến môi trường. Tạo file `.env` hoặc cấu hình trong IDE run configuration:

```env
# ── Database ──────────────────────────────────────────────────
DATABASE_URL=jdbc:postgresql://localhost:5432/easymall
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# ── JWT ───────────────────────────────────────────────────────
# Khóa bí mật HS256, tối thiểu 256-bit (32 ký tự base64)
JWT_SIGNER_KEY=your_jwt_secret_key_at_least_32_chars

# ── Email (Gmail SMTP) ────────────────────────────────────────
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password   # App Password, không phải mật khẩu Gmail

# ── Google API ────────────────────────────────────────────────
GOOGLE_API_KEY=your_google_api_key

# ── VNPay ─────────────────────────────────────────────────────
VNPAY_TMN_CODE=your_tmn_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:8080/easymall/payment/vnpay/return
VNPAY_API_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_IPN_URL=http://your-public-url/easymall/payment/vnpay/ipn
```

> ⚠️ **Không commit file `.env` lên git.** File này đã được thêm vào `.gitignore`.

---

## Database Migration

Dự án dùng **Flyway** để quản lý schema. Migration tự động chạy khi khởi động ứng dụng.

### Lịch sử migration

| Version | Mô tả |
|---|---|
| `V0` | Tạo trigger functions chung |
| `V1` | Tạo bảng `roles` |
| `V1.1` | Tạo bảng `users` |
| `V1.2` | Tạo bảng `tokens` |
| `V1.3` | Tạo bảng `addresses` |
| `V1.4 – V1.5` | Tạo bảng `permissions`, `role_permissions` |
| `V1.6 – V1.9` | Refactor tokens, seed roles mặc định |
| `V2.0` | Seed permissions & roles admin |
| `V2.1` | Tạo bảng `categories` |
| `V2.1.1` | Cài extension `unaccent` (Full-text Search tiếng Việt) |
| `V2.2` | Tạo bảng `products` + trigger full-text search |
| `V2.3` | Tạo bảng `product_variants`, `product_images` |
| `V2.4` | Tạo bảng `sliders` |
| `V2.5` | Tạo bảng `inventory_transactions` |
| `V2.6` | Seed permissions cho Product module |
| `V2.7` | Seed dữ liệu mẫu: categories, products, variants, images |

---

## API Overview

Base URL: `http://localhost:8080/easymall/api/v1`

### Authentication

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/auth/register` | Đăng ký tài khoản | Public |
| `POST` | `/auth/login` | Đăng nhập, nhận JWT | Public |
| `POST` | `/auth/refresh` | Làm mới Access Token | Public |
| `POST` | `/auth/logout` | Đăng xuất (revoke token) | Bearer |
| `POST` | `/auth/change-password` | Đổi mật khẩu | Bearer |

### Products

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `GET` | `/products` | Danh sách sản phẩm (phân trang, lọc) | Public |
| `GET` | `/products/{id}` | Chi tiết sản phẩm | Public |
| `POST` | `/products` | Tạo sản phẩm mới | Admin |
| `PUT` | `/products/{id}` | Cập nhật sản phẩm | Admin |
| `DELETE` | `/products/{id}` | Xóa mềm sản phẩm | Admin |
| `PATCH` | `/products/{id}/stock` | Cập nhật trạng thái tồn kho | Admin |

### Categories

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `GET` | `/categories` | Danh sách danh mục | Public |
| `GET` | `/categories/{id}` | Chi tiết danh mục | Public |
| `POST` | `/categories` | Tạo danh mục | Admin |
| `PUT` | `/categories/{id}` | Cập nhật danh mục | Admin |
| `DELETE` | `/categories/{id}` | Xóa danh mục | Admin |

### Users & RBAC

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `GET` | `/users` | Danh sách người dùng | Admin |
| `GET` | `/users/me` | Thông tin tài khoản hiện tại | Bearer |
| `PUT` | `/users/{id}` | Cập nhật người dùng | Admin |
| `GET` | `/roles` | Danh sách roles | Admin |
| `POST` | `/roles` | Tạo role mới | Admin |
| `GET` | `/permissions` | Danh sách permissions | Admin |

### Response Format

Tất cả API đều trả về cùng một cấu trúc chuẩn:

```json
{
  "code": 1000,
  "message": "Success",
  "result": { ... }
}
```

Khi có lỗi:

```json
{
  "code": 4001,
  "message": "Resource not found"
}
```

---

## Cấu trúc module

### SKU Format

Mã SKU biến thể sản phẩm theo chuẩn:

```
{CATEGORY_CODE}-{PRODUCT_CODE}-{ATTRIBUTE_CODE}
Ví dụ: SHIRT-BLK-L, PANT-WHT-M
```

### Phân quyền (RBAC)

Hệ thống sử dụng permission-based access control. Mỗi endpoint được bảo vệ bởi scope permission cụ thể:

| Permission | Mô tả |
|---|---|
| `product:read` | Xem sản phẩm (public) |
| `product:create` | Tạo sản phẩm |
| `product:update` | Cập nhật sản phẩm |
| `product:delete` | Xóa sản phẩm |
| `category:read` | Xem danh mục |
| `category:create` | Tạo danh mục |
| `user:read` | Xem người dùng |
| `role:manage` | Quản lý roles |

---

## Tác giả

**Quoc Nguyen Van Anh** — Đồ án tốt nghiệp DATN  
Trường: *(Tên trường)*  
Năm: 2026
