package com.quocnva.easymall.dtos.response.contact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private Long messageId;
    private Long userId; // Tùy chọn, để FE biết thư của ai
    private String guestName;
    private String guestEmail;
    private String subject;
    private String content;
    private String status;
    private OffsetDateTime createdAt;
}
