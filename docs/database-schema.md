# EasyMall — Database Schema Reference

> **Database:** PostgreSQL\
> \***\*Migrations:** Flyway (prefix `V`)\
> \***\*Last updated:** V4.8

---

## Table of Contents

| \#  | Table                                                    | Domain       | Migration   |
| --- | -------------------------------------------------------- | ------------ | ----------- |
| 1   | [roles](#1-roles)                                        | Auth         | V1          |
| 2   | [permissions](#2-permissions)                            | Auth         | V1.4        |
| 3   | [role_permissions](#3-role_permissions)                  | Auth         | V1.5        |
| 4   | [users](#4-users)                                        | Auth         | V1.1 + V1.6 |
| 5   | [tokens](#5-tokens)                                      | Auth         | V1.2 + V1.7 |
| 6   | [device_sessions](#6-device_sessions)                    | Auth / Fraud | V3          |
| 7   | [categories](#7-categories)                              | Catalog      | V2.1        |
| 8   | [products](#8-products)                                  | Catalog      | V2.2        |
| 9   | [product_variants](#9-product_variants)                  | Catalog      | V2.3        |
| 10  | [product_images](#10-product_images)                     | Catalog      | V2.3        |
| 11  | [inventory_transactions](#11-inventory_transactions)     | Catalog      | V2.5        |
| 12  | [sliders](#12-sliders)                                   | CMS          | V2.4        |
| 13  | [carts](#13-carts)                                       | Commerce     | V2.8        |
| 14  | [cart_items](#14-cart_items)                             | Commerce     | V2.8        |
| 15  | [coupons](#15-coupons)                                   | Commerce     | V3.1        |
| 16  | [orders](#16-orders)                                     | Commerce     | V3.2        |
| 17  | [order_details](#17-order_details)                       | Commerce     | V3.3        |
| 18  | [coupon_usages](#18-coupon_usages)                       | Commerce     | V3.4        |
| 19  | [reviews](#19-reviews)                                   | Social       | V3.8        |
| 20  | [review_images](#20-review_images)                       | Social       | V3.8        |
| 21  | [wishlists](#21-wishlists)                               | Social       | V4.0        |
| 22  | [addresses](#22-addresses)                               | User         | V1.3 + V4.8 |
| 23  | [user_behaviors](#23-user_behaviors)                     | Analytics    | V4.1        |
| 24  | [user_stats](#24-user_stats)                             | Analytics    | V4.1        |
| 25  | [fraud_records_and_labels](#25-fraud_records_and_labels) | Fraud        | V4.2 + V4.3 |
| 26  | [fraud_rule_configs](#26-fraud_rule_configs)             | Fraud        | V4.3        |

---

## Auth & Identity

### 1. `roles`

> V1 — Nhom quyen he thong

| Column        | Type           | Nullable | Default             | Ghi chu |
| ------------- | -------------- | -------- | ------------------- | ------- |
| `role_id`     | `BIGINT`       | NO       | IDENTITY PK         |         |
| `role_name`   | `VARCHAR(50)`  | NO       | —                   | UNIQUE  |
| `description` | `VARCHAR(255)` | YES      | NULL                | V1.9    |
| `created_at`  | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` | V1.9    |

Seeded: `ROLE_ADMIN`, `ROLE_USER`

---

### 2. `permissions`

> V1.4

| Column            | Type           | Nullable | Default             | Ghi chu                                 |
| ----------------- | -------------- | -------- | ------------------- | --------------------------------------- |
| `permission_id`   | `BIGINT`       | NO       | IDENTITY PK         |                                         |
| `permission_name` | `VARCHAR(100)` | NO       | —                   | UNIQUE. VD: `user:read`, `order:delete` |
| `description`     | `VARCHAR(255)` | YES      | NULL                |                                         |
| `created_at`      | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                                         |

---

### 3. `role_permissions`

> V1.5 — N:N roles x permissions

| Column          | Type     | Nullable | Ghi chu                                                |
| --------------- | -------- | -------- | ------------------------------------------------------ |
| `role_id`       | `BIGINT` | NO       | FK -&gt; `roles.role_id` ON DELETE CASCADE             |
| `permission_id` | `BIGINT` | NO       | FK -&gt; `permissions.permission_id` ON DELETE CASCADE |

PK: `(role_id, permission_id)`

---

### 4. `users`

> V1.1 + V1.6

| Column                | Type           | Nullable | Default             | Ghi chu                                     |
| --------------------- | -------------- | -------- | ------------------- | ------------------------------------------- |
| `user_id`             | `BIGINT`       | NO       | IDENTITY PK         |                                             |
| `created_at`          | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                                             |
| `updated_at`          | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` | Trigger `trg_users_updated_at`              |
| `is_active`           | `BOOLEAN`      | YES      | `TRUE`              | Soft-disable                                |
| `email`               | `VARCHAR(100)` | NO       | —                   | UNIQUE                                      |
| `password`            | `VARCHAR(255)` | NO       | `''`                | BCrypt hash (V1.6)                          |
| `full_name`           | `VARCHAR(100)` | NO       | —                   |                                             |
| `gender`              | `SMALLINT`     | YES      | NULL                | 0=Nu, 1=Nam, 2=Khac                         |
| `facebook_account_id` | `VARCHAR(255)` | YES      | NULL                | OAuth                                       |
| `google_account_id`   | `VARCHAR(255)` | YES      | NULL                | OAuth                                       |
| `phone`               | `VARCHAR(20)`  | YES      | NULL                |                                             |
| `dob`                 | `DATE`         | YES      | NULL                | Date of birth                               |
| `role_id`             | `BIGINT`       | YES      | NULL                | FK -&gt; `roles.role_id` ON DELETE RESTRICT |

---

### 5. `tokens`

> V1.2 + V1.7

| Column          | Type            | Nullable | Default     | Ghi chu                                    |
| --------------- | --------------- | -------- | ----------- | ------------------------------------------ |
| `token_id`      | `BIGINT`        | NO       | IDENTITY PK |                                            |
| `refresh_token` | `VARCHAR(1000)` | NO       | —           | JWT string                                 |
| `is_revoked`    | `BOOLEAN`       | YES      | `FALSE`     |                                            |
| `expires_at`    | `TIMESTAMPTZ`   | YES      | NULL        | RT expiry                                  |
| `device_info`   | `VARCHAR(255)`  | YES      | NULL        | Audit                                      |
| `user_id`       | `BIGINT`        | YES      | NULL        | FK -&gt; `users.user_id` ON DELETE CASCADE |

---

### 6. `device_sessions`

> V3 — Fraud Detection

| Column               | Type           | Nullable | Default             | Ghi chu                                     |
| -------------------- | -------------- | -------- | ------------------- | ------------------------------------------- |
| `device_session_id`  | `BIGINT`       | NO       | IDENTITY PK         |                                             |
| `user_id`            | `BIGINT`       | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE SET NULL |
| `session_id`         | `VARCHAR(100)` | NO       | —                   | UNIQUE                                      |
| `ip_address`         | `VARCHAR(45)`  | NO       | —                   | IPv4/IPv6                                   |
| `user_agent`         | `VARCHAR(500)` | NO       | —                   |                                             |
| `device_fingerprint` | `VARCHAR(255)` | YES      | NULL                | Canvas/WebGL hash                           |
| `location_country`   | `VARCHAR(100)` | YES      | NULL                |                                             |
| `is_vpn_proxy`       | `BOOLEAN`      | YES      | `FALSE`             |                                             |
| `created_at`         | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                                             |

---

## Catalog

### 7. `categories`

> V2.1 — Danh muc san pham da cap

| Column               | Type           | Nullable | Default      | Ghi chu                                              |
| -------------------- | -------------- | -------- | ------------ | ---------------------------------------------------- |
| `category_id`        | `BIGINT`       | NO       | IDENTITY PK  |                                                      |
| `category_code`      | `VARCHAR(100)` | NO       | —            | UNIQUE                                               |
| `category_name`      | `VARCHAR(200)` | NO       | —            |                                                      |
| `category_status`    | `SMALLINT`     | YES      | `1`          | 0=An, 1=Hien thi                                     |
| `level`              | `INT`          | YES      | `1`          | 1=Cha, 2=Con                                         |
| `parent_id`          | `BIGINT`       | YES      | NULL         | FK -&gt; `categories.category_id` ON DELETE SET NULL |
| `icon_url`           | `VARCHAR(500)` | YES      | NULL         |                                                      |
| `target_demographic` | `SMALLINT`     | YES      | NULL         |                                                      |
| `category_type`      | `VARCHAR(30)`  | NO       | `'STANDARD'` |                                                      |
| `display_order`      | `INT`          | NO       | `0`          |                                                      |

Index: `idx_categories_demographic (target_demographic, category_type)`

---

### 8. `products`

> V2.2

| Column                | Type           | Nullable | Default             | Ghi chu                                              |
| --------------------- | -------------- | -------- | ------------------- | ---------------------------------------------------- |
| `product_id`          | `BIGINT`       | NO       | IDENTITY PK         |                                                      |
| `product_slug`        | `VARCHAR(100)` | NO       | —                   | UNIQUE                                               |
| `product_name`        | `VARCHAR(150)` | NO       | —                   |                                                      |
| `product_description` | `TEXT`         | YES      | NULL                |                                                      |
| `in_popular`          | `BOOLEAN`      | YES      | `FALSE`             |                                                      |
| `in_stock`            | `BOOLEAN`      | YES      | `TRUE`              | Soft-delete flag                                     |
| `target_gender`       | `SMALLINT`     | YES      | `2`                 | 0=Nu, 1=Nam, 2=Unisex                                |
| `max_order_quantity`  | `INT`          | YES      | `0`                 |                                                      |
| `options_config`      | `JSONB`        | YES      | NULL                | VD: `{"sizes":["S","M"],"colors":["Do"]}`            |
| `product_tags`        | `JSONB`        | YES      | `'[]'::jsonb`       | VD: `["vintage","oversize"]`                         |
| `category_id`         | `BIGINT`       | YES      | NULL                | FK -&gt; `categories.category_id` ON DELETE SET NULL |
| `weight_kg`           | `DECIMAL(5,2)` | YES      | `0.10`              | Trong luong thuc, phuc vu GHN                        |
| `length_m`            | `DECIMAL(5,2)` | YES      | `0.10`              |                                                      |
| `width_m`             | `DECIMAL(5,2)` | YES      | `0.10`              |                                                      |
| `height_m`            | `DECIMAL(5,2)` | YES      | `0.10`              |                                                      |
| `created_at`          | `TIMESTAMP`    | YES      | `CURRENT_TIMESTAMP` |                                                      |
| `search_vector`       | `TSVECTOR`     | YES      | NULL                | Auto-updated by trigger `tsvectorupdate`             |

Indexes:

- `idx_products_search` — GIN tren `search_vector` (Full-text Search)
- `idx_products_product_tags_gin` — GIN `jsonb_path_ops` tren `product_tags`
- `idx_products_category` — BTree tren `category_id`

---

### 9. `product_variants`

> V2.3

| Column               | Type            | Nullable | Default     | Ghi chu                                          |
| -------------------- | --------------- | -------- | ----------- | ------------------------------------------------ |
| `variant_id`         | `BIGINT`        | NO       | IDENTITY PK |                                                  |
| `product_id`         | `BIGINT`        | YES      | NULL        | FK -&gt; `products.product_id` ON DELETE CASCADE |
| `price`              | `NUMERIC(15,2)` | NO       | —           | Gia ban                                          |
| `cost_price`         | `NUMERIC(15,2)` | NO       | —           | Gia von                                          |
| `variant_attributes` | `JSONB`         | NO       | —           | VD: `{"color":"Do","size":"M"}`                  |
| `sku_code`           | `VARCHAR(50)`   | NO       | —           | UNIQUE                                           |
| `variant_image`      | `VARCHAR(500)`  | YES      | NULL        |                                                  |
| `stock_quantity`     | `INT`           | YES      | `0`         | CHECK &gt;= 0                                    |
| `is_active`          | `BOOLEAN`       | NO       | `TRUE`      |                                                  |
| `locked_stock`       | `INT`           | NO       | `0`         | So luong bi giu, CHECK &gt;= 0                   |

---

### 10. `product_images`

> V2.3

| Column          | Type           | Nullable | Default     | Ghi chu                                          |
| --------------- | -------------- | -------- | ----------- | ------------------------------------------------ |
| `image_id`      | `BIGINT`       | NO       | IDENTITY PK |                                                  |
| `product_id`    | `BIGINT`       | YES      | NULL        | FK -&gt; `products.product_id` ON DELETE CASCADE |
| `image_url`     | `VARCHAR(500)` | NO       | —           | S3 path                                          |
| `is_thumbnail`  | `BOOLEAN`      | YES      | `FALSE`     | Anh bia chinh                                    |
| `display_order` | `INT`          | YES      | `0`         |                                                  |

---

### 11. `inventory_transactions`

> V2.5

| Column             | Type          | Nullable | Default             | Ghi chu                                                  |
| ------------------ | ------------- | -------- | ------------------- | -------------------------------------------------------- |
| `transaction_id`   | `BIGINT`      | NO       | IDENTITY PK         |                                                          |
| `variant_id`       | `BIGINT`      | NO       | —                   | FK -&gt; `product_variants.variant_id` ON DELETE CASCADE |
| `quantity_change`  | `INT`         | NO       | —                   | Am = xuat kho, Duong = nhap kho                          |
| `transaction_type` | `VARCHAR(50)` | NO       | —                   | `IMPORT`, `ORDER`, `RETURN`, `ADJUSTMENT`                |
| `reference_id`     | `BIGINT`      | YES      | NULL                | order_id / return_id                                     |
| `created_at`       | `TIMESTAMPTZ` | YES      | `CURRENT_TIMESTAMP` |                                                          |

---

### 12. `sliders`

> V2.4

| Column          | Type           | Nullable | Default     | Ghi chu |
| --------------- | -------------- | -------- | ----------- | ------- |
| `slider_id`     | `BIGINT`       | NO       | IDENTITY PK |         |
| `image_url`     | `VARCHAR(500)` | NO       | —           |         |
| `target_url`    | `VARCHAR(500)` | YES      | NULL        |         |
| `is_active`     | `BOOLEAN`      | NO       | `TRUE`      |         |
| `display_order` | `INT`          | NO       | `0`         |         |

---

## Commerce

### 13. `carts`

> V2.8

| Column      | Type      | Nullable | Default     | Ghi chu                                    |
| ----------- | --------- | -------- | ----------- | ------------------------------------------ |
| `cart_id`   | `BIGINT`  | NO       | IDENTITY PK |                                            |
| `is_active` | `BOOLEAN` | YES      | `TRUE`      |                                            |
| `user_id`   | `BIGINT`  | YES      | NULL        | FK -&gt; `users.user_id` ON DELETE CASCADE |

---

### 14. `cart_items`

> V2.8

| Column         | Type            | Nullable | Default     | Ghi chu                                                  |
| -------------- | --------------- | -------- | ----------- | -------------------------------------------------------- |
| `cart_item_id` | `BIGINT`        | NO       | IDENTITY PK |                                                          |
| `cart_id`      | `BIGINT`        | YES      | NULL        | FK -&gt; `carts.cart_id` ON DELETE CASCADE               |
| `variant_id`   | `BIGINT`        | YES      | NULL        | FK -&gt; `product_variants.variant_id` ON DELETE CASCADE |
| `quantity`     | `INT`           | NO       | —           | CHECK &gt; 0                                             |
| `total_money`  | `DECIMAL(12,2)` | NO       | —           |                                                          |
| `note`         | `VARCHAR(200)`  | YES      | NULL        |                                                          |

---

### 15. `coupons`

> V3.1

| Column                  | Type            | Nullable | Default             | Ghi chu                                 |
| ----------------------- | --------------- | -------- | ------------------- | --------------------------------------- |
| `coupon_id`             | `BIGINT`        | NO       | ALWAYS IDENTITY PK  |                                         |
| `code`                  | `VARCHAR(50)`   | NO       | —                   | UNIQUE                                  |
| `description`           | `VARCHAR(255)`  | YES      | NULL                |                                         |
| `discount_type`         | `VARCHAR(20)`   | NO       | —                   | `PERCENTAGE` / `FIXED_AMOUNT`           |
| `discount_value`        | `NUMERIC(15,2)` | NO       | —                   |                                         |
| `max_discount_amount`   | `NUMERIC(15,2)` | YES      | NULL                | Tran giam khi PERCENTAGE                |
| `min_order_amount`      | `NUMERIC(15,2)` | YES      | `0`                 |                                         |
| `max_usage`             | `INT`           | YES      | `1000`              | Tong luot dung                          |
| `user_usage_limit`      | `INT`           | YES      | `1`                 | Moi user toi da                         |
| `start_date`            | `TIMESTAMPTZ`   | NO       | —                   |                                         |
| `end_date`              | `TIMESTAMPTZ`   | NO       | —                   |                                         |
| `is_active`             | `BOOLEAN`       | YES      | `TRUE`              |                                         |
| `applicable_conditions` | `JSONB`         | YES      | `'{}'::jsonb`       | VD: `{"applicable_category_ids":[1,2]}` |
| `coupon_type`           | `VARCHAR(30)`   | NO       | `'SHOP_VOUCHER'`    | `SHOP_VOUCHER`, `FREE_SHIPPING`         |
| `created_at`            | `TIMESTAMPTZ`   | YES      | `CURRENT_TIMESTAMP` |                                         |

---

### 16. `orders`

> V3.2

| Column                     | Type            | Nullable | Default        | Ghi chu                                                  |
| -------------------------- | --------------- | -------- | -------------- | -------------------------------------------------------- |
| `order_id`                 | `BIGINT`        | NO       | IDENTITY PK    |                                                          |
| `order_date`               | `DATE`          | NO       | `CURRENT_DATE` |                                                          |
| `shipping_date`            | `DATE`          | YES      | NULL           |                                                          |
| `payment_method`           | `SMALLINT`      | NO       | —              | 0=COD, 1=VNPAY, 2=MOMO                                   |
| `shipping_method`          | `SMALLINT`      | NO       | —              | 0=Nhanh, 1=Hoa toc                                       |
| `delivery_order_id`        | `VARCHAR(50)`   | YES      | NULL           | Ma don GHN/Ahamove                                       |
| `delivery_status`          | `VARCHAR(50)`   | YES      | `'PENDING'`    | Trang thai giao hang tu GHN                              |
| `tracking_number`          | `VARCHAR(100)`  | NO       | `''`           | GHN order_code                                           |
| `note`                     | `VARCHAR(200)`  | YES      | NULL           |                                                          |
| `address_id`               | `BIGINT`        | YES      | NULL           | FK -&gt; `addresses.address_id` ON DELETE RESTRICT       |
| `user_id`                  | `BIGINT`        | YES      | NULL           | FK -&gt; `users.user_id` ON DELETE SET NULL              |
| `order_status`             | `VARCHAR(50)`   | NO       | `'PENDING'`    | `PENDING`,`CONFIRMED`,`SHIPPING`,`COMPLETED`,`CANCELLED` |
| `device_session_id`        | `BIGINT`        | YES      | NULL           | FK -&gt; `device_sessions.device_session_id`             |
| `total_product_money`      | `NUMERIC(15,2)` | NO       | —              | Tong tien hang goc                                       |
| `shop_discount_amount`     | `NUMERIC(15,2)` | YES      | `0.00`         | Giam tu ma Shop                                          |
| `original_shipping_fee`    | `NUMERIC(15,2)` | YES      | `0.00`         | Phi ship goc tu GHN                                      |
| `shipping_discount_amount` | `NUMERIC(15,2)` | YES      | `0.00`         | Giam tu ma Freeship                                      |
| `payment_discount_amount`  | `NUMERIC(15,2)` | YES      | `0.00`         | Giam tu ma VNPAY/MOMO                                    |
| `final_payment_money`      | `NUMERIC(15,2)` | NO       | —              | **So tien cuoi cung khach tra**                          |

---

### 17. `order_details`

> V3.3

| Column               | Type            | Nullable | Default     | Ghi chu                                                   |
| -------------------- | --------------- | -------- | ----------- | --------------------------------------------------------- |
| `order_detail_id`    | `BIGINT`        | NO       | IDENTITY PK |                                                           |
| `num_of_product`     | `INT`           | NO       | —           | So luong                                                  |
| `order_detail_price` | `DECIMAL(12,2)` | NO       | —           | Don gia tai thoi diem dat                                 |
| `total_money`        | `DECIMAL(12,2)` | NO       | —           | num_of_product x order_detail_price                       |
| `item_status`        | `VARCHAR(30)`   | NO       | `'NORMAL'`  | `NORMAL`, `RETURNED`                                      |
| `order_id`           | `BIGINT`        | YES      | NULL        | FK -&gt; `orders.order_id` ON DELETE CASCADE              |
| `variant_id`         | `BIGINT`        | YES      | NULL        | FK -&gt; `product_variants.variant_id` ON DELETE RESTRICT |

---

### 18. `coupon_usages`

> V3.4

| Column            | Type          | Nullable | Default             | Ghi chu                                        |
| ----------------- | ------------- | -------- | ------------------- | ---------------------------------------------- |
| `coupon_usage_id` | `BIGINT`      | NO       | IDENTITY PK         |                                                |
| `user_id`         | `BIGINT`      | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE CASCADE     |
| `coupon_id`       | `BIGINT`      | YES      | NULL                | FK -&gt; `coupons.coupon_id` ON DELETE CASCADE |
| `order_id`        | `BIGINT`      | YES      | NULL                | FK -&gt; `orders.order_id` ON DELETE CASCADE   |
| `used_at`         | `TIMESTAMPTZ` | NO       | `CURRENT_TIMESTAMP` |                                                |

Constraint: `UNIQUE (user_id, coupon_id, order_id)`

---

## Social

### 19. `reviews`

> V3.8

| Column          | Type          | Nullable | Default             | Ghi chu                                                             |
| --------------- | ------------- | -------- | ------------------- | ------------------------------------------------------------------- |
| `review_id`     | `BIGINT`      | NO       | IDENTITY PK         |                                                                     |
| `user_id`       | `BIGINT`      | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE CASCADE                          |
| `product_id`    | `BIGINT`      | YES      | NULL                | FK -&gt; `products.product_id` ON DELETE CASCADE                    |
| `order_id`      | `BIGINT`      | YES      | NULL                | FK -&gt; `orders.order_id` ON DELETE CASCADE. NULL khi order bi xoa |
| `rating`        | `INT`         | NO       | —                   | CHECK 1 &lt;= rating &lt;= 5                                        |
| `comment`       | `TEXT`        | YES      | NULL                |                                                                     |
| `review_status` | `VARCHAR(20)` | NO       | `'PENDING'`         | `PENDING`, `APPROVED`, `HIDDEN`                                     |
| `reply_at`      | `TIMESTAMPTZ` | YES      | NULL                |                                                                     |
| `created_at`    | `TIMESTAMPTZ` | NO       | `CURRENT_TIMESTAMP` |                                                                     |
| `updated_at`    | `TIMESTAMPTZ` | NO       | `CURRENT_TIMESTAMP` | Trigger `trg_reviews_updated_at`                                    |

---

### 20. `review_images`

> V3.8

| Column            | Type           | Nullable | Ghi chu                                        |
| ----------------- | -------------- | -------- | ---------------------------------------------- |
| `review_image_id` | `BIGINT`       | NO       | IDENTITY PK                                    |
| `review_id`       | `BIGINT`       | YES      | FK -&gt; `reviews.review_id` ON DELETE CASCADE |
| `image_url`       | `VARCHAR(500)` | NO       | Link Cloudinary                                |

---

### 21. `wishlists`

> V4.0

| Column        | Type          | Nullable | Default             | Ghi chu                                          |
| ------------- | ------------- | -------- | ------------------- | ------------------------------------------------ |
| `wishlist_id` | `BIGINT`      | NO       | IDENTITY PK         |                                                  |
| `user_id`     | `BIGINT`      | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE CASCADE       |
| `product_id`  | `BIGINT`      | YES      | NULL                | FK -&gt; `products.product_id` ON DELETE CASCADE |
| `created_at`  | `TIMESTAMPTZ` | NO       | `CURRENT_TIMESTAMP` |                                                  |
| `updated_at`  | `TIMESTAMPTZ` | NO       | `CURRENT_TIMESTAMP` | Trigger `trg_wishlists_updated_at`               |

---

## User

### 22. `addresses`

> V1.3 + V4.8

| Column           | Type           | Nullable | Default     | Ghi chu                                    |
| ---------------- | -------------- | -------- | ----------- | ------------------------------------------ |
| `address_id`     | `BIGINT`       | NO       | IDENTITY PK |                                            |
| `recipient_name` | `VARCHAR(100)` | NO       | —           |                                            |
| `phone`          | `VARCHAR(15)`  | NO       | —           |                                            |
| `province_id`    | `INT`          | NO       | —           | GHN province_id                            |
| `district_id`    | `INT`          | NO       | —           | GHN district_id                            |
| `ward_code`      | `VARCHAR(20)`  | NO       | —           | GHN ward_code                              |
| `province_name`  | `VARCHAR(100)` | YES      | NULL        | Ten tinh (V4.8)                            |
| `district_name`  | `VARCHAR(100)` | YES      | NULL        | Ten huyen (V4.8)                           |
| `ward_name`      | `VARCHAR(100)` | YES      | NULL        | Ten xa (V4.8)                              |
| `full_address`   | `VARCHAR(255)` | YES      | NULL        |                                            |
| `street_number`  | `VARCHAR(100)` | YES      | NULL        | So nha / duong                             |
| `is_default`     | `BOOLEAN`      | YES      | `FALSE`     |                                            |
| `user_id`        | `BIGINT`       | YES      | NULL        | FK -&gt; `users.user_id` ON DELETE CASCADE |

---

## Analytics & Fraud

### 23. `user_behaviors`

> V4.1

| Column             | Type           | Nullable | Default             | Ghi chu                                                    |
| ------------------ | -------------- | -------- | ------------------- | ---------------------------------------------------------- |
| `user_behavior_id` | `BIGINT`       | NO       | ALWAYS IDENTITY PK  |                                                            |
| `user_id`          | `BIGINT`       | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE SET NULL (NULL = Guest) |
| `session_id`       | `VARCHAR(100)` | NO       | —                   |                                                            |
| `product_id`       | `BIGINT`       | YES      | NULL                | FK -&gt; `products.product_id` ON DELETE SET NULL          |
| `category_id`      | `BIGINT`       | YES      | NULL                | FK -&gt; `categories.category_id` ON DELETE SET NULL       |
| `action_type`      | `VARCHAR(50)`  | NO       | —                   | `VIEW`,`CLICK`,`ADD_TO_CART`,`PURCHASE`,`RATE`             |
| `keyword`          | `VARCHAR(255)` | YES      | NULL                | Tu khoa tim kiem                                           |
| `context_data`     | `JSONB`        | YES      | NULL                | VD: `{"device":"mobile","duration_seconds":45}`            |
| `variant_id`       | `BIGINT`       | YES      | NULL                | FK -&gt; `product_variants.variant_id`                     |
| `created_at`       | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                                                            |

Indexes: `idx_behavior_user`, `idx_behavior_product`, `idx_behavior_category`, `idx_behavior_context` (GIN)

---

### 24. `user_stats`

> V4.1

| Column                        | Type           | Nullable | Default             | Ghi chu                                    |
| ----------------------------- | -------------- | -------- | ------------------- | ------------------------------------------ |
| `user_id`                     | `BIGINT`       | NO       | PK                  | FK -&gt; `users.user_id` ON DELETE CASCADE |
| `total_orders`                | `INT`          | YES      | `0`                 |                                            |
| `returned_orders_count`       | `INT`          | YES      | `0`                 |                                            |
| `reputation_score`            | `DECIMAL(5,2)` | YES      | `100.00`            |                                            |
| `is_restricted`               | `BOOLEAN`      | YES      | `FALSE`             |                                            |
| `account_age_days`            | `INT`          | YES      | `0`                 | Phong chong tai khoan clone                |
| `failed_payment_attempts_10m` | `INT`          | YES      | `0`                 | Dau hieu card testing                      |
| `total_distinct_devices`      | `INT`          | YES      | `1`                 | So thiet bi login                          |
| `updated_at`                  | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` | Trigger `trg_user_stats_updated_at`        |

---

### 25. `fraud_records_and_labels`

> V4.2 + V4.3

| Column             | Type           | Nullable | Default             | Ghi chu                                              |
| ------------------ | -------------- | -------- | ------------------- | ---------------------------------------------------- |
| `fraud_record_id`  | `BIGINT`       | NO       | IDENTITY PK         |                                                      |
| `order_id`         | `BIGINT`       | NO       | —                   | FK -&gt; `orders.order_id` ON DELETE CASCADE. UNIQUE |
| `risk_score`       | `DECIMAL(5,2)` | NO       | —                   | AI score 0.00 - 100.00                               |
| `system_decision`  | `VARCHAR(50)`  | NO       | —                   | `APPROVE`, `REVIEW`, `DECLINE`                       |
| `final_label`      | `VARCHAR(100)` | NO       | `'PENDING'`         | Ground truth label                                   |
| `labeled_by`       | `BIGINT`       | YES      | NULL                | FK -&gt; `users.user_id` ON DELETE SET NULL          |
| `analyst_notes`    | `TEXT`         | YES      | NULL                |                                                      |
| `top_risk_factors` | `JSONB`        | YES      | NULL                | VD: `["order_total>50M","is_vpn=1"]` (V4.3)          |
| `created_at`       | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                                                      |
| `updated_at`       | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` | Trigger `trg_fraud_labels_updated_at`                |

---

### 26. `fraud_rule_configs`

> V4.3 — Singleton config

| Column              | Type           | Nullable | Default             | Ghi chu                  |
| ------------------- | -------------- | -------- | ------------------- | ------------------------ |
| `config_id`         | `INT`          | NO       | PK                  | Luon la 1                |
| `review_threshold`  | `DECIMAL(5,2)` | NO       | `40.00`             | Nguong can review        |
| `decline_threshold` | `DECIMAL(5,2)` | NO       | `75.00`             | Nguong tu choi           |
| `is_active`         | `BOOLEAN`      | YES      | `TRUE`              |                          |
| `updated_at`        | `TIMESTAMPTZ`  | YES      | `CURRENT_TIMESTAMP` |                          |
| `updated_by`        | `BIGINT`       | YES      | NULL                | FK -&gt; `users.user_id` |

---

## Migration History

| Version | File                                                       | Noi dung                                                                               |
| ------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| V0      | `V0__create_trigger_functions.sql`                         | Trigger function `trigger_set_timestamp()`                                             |
| V1      | `V1__create_roles_table.sql`                               | Bang `roles`                                                                           |
| V1.1    | `V1.1__create_users_table.sql`                             | Bang `users`                                                                           |
| V1.2    | `V1.2__create_tokens_table.sql`                            | Bang `tokens`                                                                          |
| V1.3    | `V1.3__create_addresses_table.sql`                         | Bang `addresses`                                                                       |
| V1.4    | `V1.4__create_permissions_table.sql`                       | Bang `permissions`                                                                     |
| V1.5    | `V1.5__create_role_permissions_table.sql`                  | Bang `role_permissions`                                                                |
| V1.6    | `V1.6__add_password_to_users.sql`                          | Them cot `password` vao `users`                                                        |
| V1.7    | `V1.7__refactor_tokens_table.sql`                          | Doi ten `token_value` -&gt; `refresh_token`, them `expires_at`, `device_info`          |
| V1.8    | `V1.8__seed_default_roles.sql`                             | Seed `ROLE_ADMIN`, `ROLE_USER`                                                         |
| V1.9    | `V1.9__alter_roles_add_columns.sql`                        | Them `description`, `created_at` vao `roles`                                           |
| V2.0    | `V2.0__seed_permissions_roles_admin.sql`                   | Seed permissions + role_permissions cho ADMIN                                          |
| V2.1.1  | `V2.1.1__install_extensions.sql`                           | Extension `unaccent`, `pg_trgm`                                                        |
| V2.1    | `V2.1__create_catgories_table.sql`                         | Bang `categories`                                                                      |
| V2.2    | `V2.2__create_product_table.sql`                           | Bang `products` + FTS trigger + indexes                                                |
| V2.3    | `V2.3__create_product_variants_and_images_table.sql`       | Bang `product_variants`, `product_images`                                              |
| V2.4    | `V2.4__create_slider_table.sql`                            | Bang `sliders`                                                                         |
| V2.5    | `V2.5__create_inventory_transaction_table.sql`             | Bang `inventory_transactions`                                                          |
| V2.6    | `V2.6__seed_product_permissions.sql`                       | Seed product permissions                                                               |
| V2.7    | `V2.7__seed_categories_products.sql`                       | Seed demo categories & products                                                        |
| V2.8    | `V2.8__create_cart_and_cart_items_table.sql`               | Bang `carts`, `cart_items`                                                             |
| V2.9    | `V2.9__fix_stock_constraint_and_seed_cart_permissions.sql` | Fix constraint + seed cart permissions                                                 |
| V3      | `V3__create_device_sessions_table.sql`                     | Bang `device_sessions`                                                                 |
| V3.1    | `V3.1__create_coupons_table.sql`                           | Bang `coupons`                                                                         |
| V3.2    | `V3.2__create_order_table.sql`                             | Bang `orders`                                                                          |
| V3.3    | `V3.3__create_order_details_table.sql`                     | Bang `order_details`                                                                   |
| V3.4    | `V3.4__create_coupon_usages_table.sql`                     | Bang `coupon_usages`                                                                   |
| V3.5    | `V3.5__seed_coupon_order_permissions.sql`                  | Seed permissions                                                                       |
| V3.6    | `V3.6__seed_demo_users_and_addresses.sql`                  | Seed demo users                                                                        |
| V3.7    | `V3.7__seed_coupons_and_orders.sql`                        | Seed demo coupons & orders                                                             |
| V3.8    | `V3.8__create_reviews_and_review_images_table.sql`         | Bang `reviews`, `review_images`                                                        |
| V3.9    | `V3.9__create_inventory_transaction_table.sql`             | Idempotent re-create                                                                   |
| V4.0    | `V4.0__create_wishlists_table.sql`                         | Bang `wishlists`                                                                       |
| V4.1    | `V4.1__create_user_stats_and_user_behavior_table.sql`      | Bang `user_behaviors`, `user_stats`                                                    |
| V4.2    | `V4.2__create_fraud_records_table.sql`                     | Bang `fraud_records_and_labels`                                                        |
| V4.3    | `V4.3__create_rule_configs_table.sql`                      | Bang `fraud_rule_configs`, them `top_risk_factors`                                     |
| V4.4    | `V4.4__reseed_role_permissions.sql`                        | Re-seed role permissions                                                               |
| V4.5    | `V4.5__reseed_user_kiethuynh_and_address.sql`              | Seed test user                                                                         |
| V4.6    | `V4.6__normalize_variant_attributes_keys.sql`              | Chuan hoa key JSONB `variant_attributes`                                               |
| V4.7    | `V4.7__setup_review_wishlist.sql`                          | Seed review/wishlist permissions + data                                                |
| V4.8    | `V4.8__add_address_ghn_fields.sql`                         | Them `province_name`, `district_name`, `ward_name` + seed `address:manage`, `ghn:read` |
