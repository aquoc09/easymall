-- V5.14: PH6 — Bổ sung UNIQUE constraint cho reviews
-- Đảm bảo mỗi user chỉ review 1 lần cho mỗi (user, product, order)
-- Baseline V3.8 chưa có constraint này.

ALTER TABLE reviews
    ADD CONSTRAINT uq_reviews_user_product_order
    UNIQUE (user_id, product_id, order_id);
