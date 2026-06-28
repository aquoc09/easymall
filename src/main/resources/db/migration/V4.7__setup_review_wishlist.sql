-- V4.7__setup_review_wishlist.sql
-- 1. Alter reviews FK: order_id ON DELETE SET NULL (review sẽ anonymous khi order bị xóa)
-- 2. Thêm UNIQUE constraint cho wishlists(user_id, product_id)
-- 3. Seed permissions cho Review và Wishlist

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 1: Alter reviews.order_id FK → SET NULL
-- ──────────────────────────────────────────────────────────────────────

-- Cho phép order_id = NULL (anonymous review)
ALTER TABLE reviews ALTER COLUMN order_id DROP NOT NULL;

-- Drop constraint cũ (CASCADE), thêm lại với SET NULL
ALTER TABLE reviews
    DROP CONSTRAINT IF EXISTS reviews_order_id_fkey;

ALTER TABLE reviews
    ADD CONSTRAINT reviews_order_id_fkey
        FOREIGN KEY (order_id)
            REFERENCES orders (order_id)
            ON DELETE SET NULL;

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 2: Unique constraint cho wishlists
-- ──────────────────────────────────────────────────────────────────────

ALTER TABLE wishlists
    ADD CONSTRAINT uq_wishlists_user_product
        UNIQUE (user_id, product_id);

-- ──────────────────────────────────────────────────────────────────────
-- SECTION 3: Seed permissions
-- ──────────────────────────────────────────────────────────────────────

INSERT INTO permissions (permission_name)
VALUES
    ('review:create'),
    ('review:view'),
    ('review:moderate'),
    ('review:delete'),
    ('wishlist:view'),
    ('wishlist:manage')
ON CONFLICT (permission_name) DO NOTHING;

-- ROLE_USER: tạo, xem, xóa review của chính mình + quản lý wishlist
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_USER'
  AND p.permission_name IN (
      'review:create',
      'review:view',
      'review:delete',
      'wishlist:view',
      'wishlist:manage'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ROLE_ADMIN: tất cả permissions trên + review:moderate
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.role_name = 'ROLE_ADMIN'
  AND p.permission_name IN (
      'review:create',
      'review:view',
      'review:moderate',
      'review:delete',
      'wishlist:view',
      'wishlist:manage'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
