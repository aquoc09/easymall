package com.quocnva.easymall.dtos.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {

    @NotBlank(message = "{validation.categoryName.not-blank}")
    @Size(max = 200, message = "{validation.categoryName.size}")
    private String categoryName;

    @NotNull(message = "{validation.categoryStatus.not-null}")
    private Integer categoryStatus;

    @Size(max = 500, message = "{validation.iconUrl.size}")
    private String iconUrl;

    private Integer targetDemographic;

    @Size(max = 30, message = "{validation.categoryType.size}")
    private String categoryType;

    private Integer displayOrder;

}
