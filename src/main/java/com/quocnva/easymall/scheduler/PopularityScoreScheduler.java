package com.quocnva.easymall.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * PopularityScoreScheduler — Batch Job tính lại popularity_score cho toàn bộ sản phẩm active.
 *
 * Công thức:
 *   popularity_score = (0.1 × LN(view_count+1)) + (0.6 × LN(sold_count+1))
 *                    + (0.3 × rating_avg × LN(rating_count+1))
 *
 * Cơ chế phòng vệ:
 *   - ShedLock: chống Double Execution trên Kubernetes cluster
 *   - @Retryable: tự động retry 3 lần nếu gặp deadlock / connection error (backoff exponential)
 *   - @Transactional(timeout=600): giới hạn 10 phút tránh giữ lock quá lâu
 *   - Native SQL: tính toán trong PostgreSQL, không load dữ liệu về Java RAM (tránh OOM)
 *   - COALESCE + LN(x+1): tránh ArithmeticException khi cột NULL hoặc bằng 0
 *   - @Recover: gửi alert khi thất bại hoàn toàn sau 3 lần retry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PopularityScoreScheduler {

    private final JdbcTemplate jdbcTemplate;

    // ── Main Job ──────────────────────────────────────────────────────────────

    /**
     * Chạy lúc 2:00 AM mỗi ngày.
     * lockAtLeastFor: giữ lock tối thiểu 5 phút dù job chạy nhanh (tránh race condition giữa các node khởi động lại).
     * lockAtMostFor: giải phóng lock sau tối đa 15 phút (tránh lock orphan khi node sập giữa chừng).
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "popularityScoreLock", lockAtLeastFor = "5m", lockAtMostFor = "15m")
    @Retryable(
        retryFor = { ConcurrencyFailureException.class, DataAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000, multiplier = 2.0)
    )
    @Transactional(timeout = 600)
    public void executeCalculatePopularityScore() {
        log.info("[PopularityJob] START — {}", OffsetDateTime.now());

        /*
         * Tính toán hoàn toàn bằng Native SQL trong PostgreSQL:
         *   - Không kéo data về JVM → tránh OutOfMemoryError
         *   - COALESCE(col, 0) + LN(x+1) → tránh ArithmeticException khi NULL hoặc bằng 0
         *   - Chỉ update sản phẩm in_stock = true → giảm rows xử lý, tránh DB CPU spike
         */
        String sql = """
            UPDATE products
            SET popularity_score = ROUND((
                (0.1 * LN(COALESCE(view_count,  0) + 1)) +
                (0.6 * LN(COALESCE(sold_count,  0) + 1)) +
                (0.3 * COALESCE(rating_avg, 0.0) * LN(COALESCE(rating_count, 0) + 1))
            )::NUMERIC, 4)
            WHERE in_stock = true
            """;

        try {
            int rows = jdbcTemplate.update(sql);
            log.info("[PopularityJob] SUCCESS — updated {} products", rows);
        } catch (DataAccessException e) {
            log.error("[PopularityJob] DB error — will retry: {}", e.getMessage());
            throw e; // Kích hoạt @Retryable
        } finally {
            log.info("[PopularityJob] END — {}", OffsetDateTime.now());
        }
    }

    // ── Recovery ──────────────────────────────────────────────────────────────

    /**
     * Gọi sau khi đã retry 3 lần nhưng vẫn thất bại.
     * Cần implement sendAlert() để gửi thông báo tới Telegram/Slack.
     */
    @Recover
    public void recoverDatabaseFailure(DataAccessException e) {
        log.error("[PopularityJob] FAILED after 3 retries: {}", e.getMessage());
        sendAlert("[PopularityScoreJob] FAILED: " + e.getMessage());
    }

    private void sendAlert(String message) {
        // TODO: implement Telegram / Slack webhook alert
        log.warn("[ALERT] {}", message);
    }
}
