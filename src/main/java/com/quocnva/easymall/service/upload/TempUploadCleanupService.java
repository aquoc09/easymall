package com.quocnva.easymall.service.upload;

import com.quocnva.easymall.entity.TempUploadEntity;
import com.quocnva.easymall.repository.TempUploadRepository;
import com.quocnva.easymall.util.UploadConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TempUploadCleanupService — tác vụ dọn dẹp ảnh "mồ côi" tự động.
 *
 * <p>Chạy định kỳ theo {@link UploadConstants#CLEANUP_CRON} (mặc định 2:00 AM).
 * Quét bảng {@code temp_uploads} để tìm các URL ảnh tồn tại quá
 * {@link UploadConstants#CLEANUP_THRESHOLD_HOURS} giờ mà chưa được liên kết
 * vào entity nghiệp vụ nào.
 *
 * <p>Với mỗi bản ghi tìm được:
 * <ol>
 *   <li>Xóa file vật lý trên S3.</li>
 *   <li>Xóa bản ghi trong DB.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TempUploadCleanupService {

    private final TempUploadRepository tempUploadRepository;
    private final StorageService storageService;

    @Scheduled(cron = UploadConstants.CLEANUP_CRON)
    @Transactional
    public void cleanupOrphanUploads() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusHours(UploadConstants.CLEANUP_THRESHOLD_HOURS);

        List<TempUploadEntity> orphans = tempUploadRepository.findAllByCreatedAtBefore(threshold);

        if (orphans.isEmpty()) {
            log.info("[Cleanup] No orphan uploads found.");
            return;
        }

        log.info("[Cleanup] Found {} orphan upload(s) older than {} hours.", orphans.size(), UploadConstants.CLEANUP_THRESHOLD_HOURS);

        int deleted = 0;
        for (TempUploadEntity orphan : orphans) {
            try {
                storageService.deleteByUrl(orphan.getUrl());
                tempUploadRepository.delete(orphan);
                deleted++;
            } catch (Exception e) {
                log.error("[Cleanup] Failed to delete orphan url={}: {}", orphan.getUrl(), e.getMessage());
                // Tiếp tục xử lý các bản ghi còn lại, không dừng toàn bộ job
            }
        }

        log.info("[Cleanup] Successfully cleaned up {}/{} orphan uploads.", deleted, orphans.size());
    }
}
