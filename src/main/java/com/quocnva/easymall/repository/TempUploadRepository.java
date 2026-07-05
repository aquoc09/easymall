package com.quocnva.easymall.repository;

import com.quocnva.easymall.entity.TempUploadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TempUploadRepository extends JpaRepository<TempUploadEntity, Long> {

    /**
     * Xóa bản ghi trung chuyển sau khi URL được liên kết chính thức.
     * Dùng trong ProductServiceImpl và ReviewServiceImpl.
     */
    @Modifying
    @Query("DELETE FROM TempUploadEntity t WHERE t.url = :url")
    void deleteByUrl(@Param("url") String url);

    /**
     * Tìm tất cả bản ghi cũ hơn ngưỡng thời gian — dùng cho cleanup job.
     */
    List<TempUploadEntity> findAllByCreatedAtBefore(LocalDateTime threshold);
}
