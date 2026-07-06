package com.quocnva.easymall.dtos.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer categoryStatus;
    private Integer level;
    private Long parentId;
    private String iconUrl;
    private Integer displayOrder;
    private OffsetDateTime updatedAt;
    
    // For Tree Structure
    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();

}
