package com.quocnva.easymall.dtos.request.category;

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
public class CategoryCreateRequest {

    @NotBlank(message = "{validation.categoryName.not-blank}")
    @Size(max = 200, message = "{validation.categoryName.size}")
    private String categoryName;

    private Long parentId;

    @Size(max = 500, message = "{validation.iconUrl.size}")
    private String iconUrl;

    private Integer displayOrder;

}
