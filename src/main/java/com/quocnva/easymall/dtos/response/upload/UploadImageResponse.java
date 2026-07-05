package com.quocnva.easymall.dtos.response.upload;

import lombok.*;

/**
 * Response trả về URL công khai của ảnh vừa upload lên S3.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadImageResponse {

    /** URL công khai đầy đủ trên AWS S3. */
    private String url;
}
