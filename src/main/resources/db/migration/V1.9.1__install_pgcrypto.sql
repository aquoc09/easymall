-- ==========================================
-- V1.9.1: Install pgcrypto extension
-- ==========================================

-- Enable pgcrypto for BCrypt password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;
