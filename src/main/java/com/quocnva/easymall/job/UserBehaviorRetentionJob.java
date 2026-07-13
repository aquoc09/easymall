package com.quocnva.easymall.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserBehaviorRetentionJob {

    private final JdbcTemplate jdbcTemplate;

    // Chạy vào 3:00 AM Chủ Nhật hàng tuần
    @Scheduled(cron = "0 0 3 * * SUN")
    public void purgeOldUserBehaviors() {
        log.info("Starting purge of old user_behaviors...");
        
        try {
            // Xóa dữ liệu cũ hơn 6 tháng
            int deletedRows = jdbcTemplate.update(
                "DELETE FROM user_behaviors WHERE created_at < NOW() - INTERVAL '6 months'"
            );
            log.info("Successfully deleted {} old user behaviors.", deletedRows);
        } catch (Exception e) {
            log.error("Error occurred while purging old user behaviors", e);
        }
    }
}
