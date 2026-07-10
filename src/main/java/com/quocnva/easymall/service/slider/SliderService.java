package com.quocnva.easymall.service.slider;

import com.quocnva.easymall.dtos.request.slider.SliderCreateRequest;
import com.quocnva.easymall.dtos.request.slider.SliderUpdateRequest;
import com.quocnva.easymall.dtos.response.slider.SliderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SliderService {

    SliderResponse createSlider(SliderCreateRequest request);

    SliderResponse updateSlider(Long sliderId, SliderUpdateRequest request);

    void deleteSlider(Long sliderId);

    Page<SliderResponse> getAllSliders(Pageable pageable);

    List<SliderResponse> getActiveSliders();
}
