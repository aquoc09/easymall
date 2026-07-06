package com.quocnva.easymall.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * SchedulerConfig — Cấu hình Scheduler và ShedLock.
 * ShedLock đảm bảo trong môi trường multi-node (Kubernetes), chỉ duy nhất 1 node
 * được phép chạy mỗi Batch Job tại một thời điểm.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
public class SchedulerConfig {

    /**
     * LockProvider sử dụng PostgreSQL làm distributed lock backend.
     * usingDbTime(): đồng bộ múi giờ theo đồng hồ DB, tránh drift giữa các node.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build()
        );
    }
}
