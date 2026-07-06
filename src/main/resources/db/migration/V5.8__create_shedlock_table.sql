-- ══════════════════════════════════════════════════════════════════════
-- V5.8 — Create shedlock table
-- Phục vụ ShedLock distributed lock cho @Scheduled batch jobs
-- Đảm bảo chỉ 1 node trong cluster được chạy mỗi job tại một thời điểm
-- ══════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
