package com.quocnva.easymall.util;

import java.util.Set;

/**
 * Các hằng số dùng chung cho module Upload.
 * Tập trung tại đây để tránh magic number và dễ điều chỉnh.
 */
public final class UploadConstants {

    private UploadConstants() {
    }

    /** Giới hạn kích thước file upload: 2 MB. */
    public static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;

    /** MIME types được chấp nhận. */
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    /**
     * Ngưỡng thời gian (giờ) để cleanup job xóa ảnh "mồ côi".
     * Ảnh tồn tại trong temp_uploads quá 24 giờ sẽ bị xóa khỏi S3 và DB.
     */
    public static final long CLEANUP_THRESHOLD_HOURS = 24;

    /** Cron expression cho cleanup job: chạy lúc 2:00 AM hàng ngày. */
    public static final String CLEANUP_CRON = "0 0 2 * * *";

    /** Folder prefix cho ảnh sản phẩm trên S3. */
    public static final String FOLDER_PRODUCTS = "products";

    /** Folder prefix cho ảnh review trên S3. */
    public static final String FOLDER_REVIEWS = "reviews";

    /** Folder prefix cho ảnh user trên S3. */
    public static final String FOLDER_USERS = "users";

    /** Folder prefix cho icon category trên S3. */
    public static final String FOLDER_CATEGORIES = "categories";

    /** Folder prefix cho ảnh slider trên S3. */
    public static final String FOLDER_SLIDERS = "sliders";
}

