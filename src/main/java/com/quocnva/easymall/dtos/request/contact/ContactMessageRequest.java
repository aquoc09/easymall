package com.quocnva.easymall.dtos.request.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageRequest {

    // Không dùng @NotBlank vì sẽ validate động bằng code (chỉ bắt buộc khi Guest)
    private String guestName;

    private String guestEmail;

    @NotBlank(message = "SUBJECT_IS_REQUIRED")
    @Size(max = 200, message = "SUBJECT_TOO_LONG")
    private String subject;

    @NotBlank(message = "CONTENT_IS_REQUIRED")
    @Size(min = 10, message = "CONTENT_TOO_SHORT")
    private String content;
}
