package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.request.slider.SliderCreateRequest;
import com.quocnva.easymall.dtos.request.slider.SliderUpdateRequest;
import com.quocnva.easymall.dtos.response.slider.SliderResponse;
import com.quocnva.easymall.entity.SliderEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class SliderMapper {

    @Value("${storage.base-url}")
    protected String storageBaseUrl;

    @Mapping(target = "sliderId", ignore = true)
    public abstract SliderEntity toEntity(SliderCreateRequest request);

    @Mapping(target = "sliderId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateEntityFromRequest(SliderUpdateRequest request, @MappingTarget SliderEntity entity);

    public abstract SliderResponse toResponse(SliderEntity entity);

    public abstract List<SliderResponse> toResponseList(List<SliderEntity> entities);

    @AfterMapping
    protected void afterToResponse(SliderEntity entity, @MappingTarget SliderResponse response) {
        if (entity.getImageUrl() != null && !entity.getImageUrl().startsWith("http")) {
            response.setImageUrl(storageBaseUrl + "/" + entity.getImageUrl());
        }
    }
}
