-- ══════════════════════════════════════════════════════════════════════
-- V2.1.1: Install PostgreSQL Extensions
-- Phải chạy TRƯỚC V2.2 vì trigger products_search_trigger() gọi unaccent()
-- ══════════════════════════════════════════════════════════════════════

-- Extension unaccent: loại bỏ dấu tiếng Việt cho Full-text Search
-- Được dùng trong products_search_trigger() tại V2.2
CREATE EXTENSION IF NOT EXISTS unaccent;
