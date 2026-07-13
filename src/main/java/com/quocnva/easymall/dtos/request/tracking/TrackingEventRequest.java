package com.quocnva.easymall.dtos.request.tracking;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEventRequest {

    private Long userId;

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    private Long productId;
    
    private Long categoryId;

    @NotBlank(message = "Action type is required")
    private String actionType;

    private String keyword;

    private String contextData;

    private Long variantId;

    private Integer durationSeconds;

    private String source;
}
