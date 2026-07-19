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

