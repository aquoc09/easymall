package com.quocnva.easymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_code", nullable = false, unique = true, length = 100)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 200)
    private String categoryName;

    // SMALLINT (int2) trong DB → dùng Short ở Java, KHÔNG dùng columnDefinition
    @Column(name = "category_status")
    private Short categoryStatus;

    @Column(name = "level")
    private Integer level;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    // SMALLINT (int2) trong DB → Short
    @Column(name = "target_demographic")
    private Short targetDemographic;

    @Column(name = "category_type", nullable = false, length = 30)
    private String categoryType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

}
