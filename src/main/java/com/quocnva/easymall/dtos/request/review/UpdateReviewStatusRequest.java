package com.quocnva.easymall.dtos.request.review;

import com.quocnva.easymall.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReviewStatusRequest {

    @NotNull(message = "{validation.reviewStatus.not-null}")
    private ReviewStatus status;
}
