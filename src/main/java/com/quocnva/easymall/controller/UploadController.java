package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.upload.UploadImageResponse;
import com.quocnva.easymall.service.upload.StorageService;
import com.quocnva.easymall.util.Translator;
import com.quocnva.easymall.util.UploadConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * UploadController — endpoint dùng chung để upload ảnh lên AWS S3.
 *
 * <p>Tách biệt khỏi các module nghiệp vụ (Product, Review) theo kiến trúc
 * "Decoupled Upload Flow": Frontend upload ảnh trước, nhận URL, rồi gửi
 * URL đó cùng data nghiệp vụ trong một request riêng.
 *
 * <p>Endpoint yêu cầu xác thực (JWT) — chỉ user đã đăng nhập mới upload được.
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    /**
     * Upload ảnh sản phẩm lên S3.
     *
     * <pre>
     * POST /api/v1/uploads/image/products
     * Content-Type: multipart/form-data
     * Body: file (image file)
     * </pre>
     */
    @PostMapping(value = "/image/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadImageResponse> uploadProductImage(
            @RequestPart("file") MultipartFile file) {

        UploadImageResponse response = storageService.uploadImage(file, UploadConstants.FOLDER_PRODUCTS);

        return ApiResponse.<UploadImageResponse>builder()
                .message(Translator.toLocale("success.upload.image"))
                .result(response)
                .build();
    }

    /**
     * Upload ảnh review lên S3.
     *
     * <pre>
     * POST /api/v1/uploads/image/reviews
     * Content-Type: multipart/form-data
     * Body: file (image file)
     * </pre>
     */
    @PostMapping(value = "/image/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadImageResponse> uploadReviewImage(
            @RequestPart("file") MultipartFile file) {

        UploadImageResponse response = storageService.uploadImage(file, UploadConstants.FOLDER_REVIEWS);

        return ApiResponse.<UploadImageResponse>builder()
                .message(Translator.toLocale("success.upload.image"))
                .result(response)
                .build();
    }

    /**
     * Upload avatar người dùng lên S3.
     *
     * <pre>
     * POST /api/v1/uploads/image/users
     * Content-Type: multipart/form-data
     * Body: file (image file)
     * </pre>
     *
     * <p>Sau khi upload thành công, FE nhận {@code s3Key} và gửi trong
     * {@code PATCH /api/v1/users/{id}} với field {@code avatar}.
     */
    @PostMapping(value = "/image/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadImageResponse> uploadUserAvatar(
            @RequestPart("file") MultipartFile file) {

        UploadImageResponse response = storageService.uploadImage(file, UploadConstants.FOLDER_USERS);

        return ApiResponse.<UploadImageResponse>builder()
                .message(Translator.toLocale("success.upload.image"))
                .result(response)
                .build();
    }
}
