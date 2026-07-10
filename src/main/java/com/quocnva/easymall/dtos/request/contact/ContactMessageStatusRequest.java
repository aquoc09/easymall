package com.quocnva.easymall.dtos.request.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageStatusRequest {

    @NotBlank(message = "STATUS_IS_REQUIRED")
    @Pattern(regexp = "^(RESOLVED|REJECTED)$", message = "INVALID_CONTACT_STATUS")
    private String status;
}
