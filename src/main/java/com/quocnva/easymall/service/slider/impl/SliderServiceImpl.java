package com.quocnva.easymall.service.slider.impl;

import com.quocnva.easymall.dtos.request.slider.SliderCreateRequest;
import com.quocnva.easymall.dtos.request.slider.SliderUpdateRequest;
import com.quocnva.easymall.dtos.response.slider.SliderResponse;
import com.quocnva.easymall.entity.SliderEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.SliderMapper;
import com.quocnva.easymall.repository.SliderRepository;
import com.quocnva.easymall.service.slider.SliderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SliderServiceImpl implements SliderService {

    private final SliderRepository sliderRepository;
    private final SliderMapper sliderMapper;

    @Override
    @Transactional
    public SliderResponse createSlider(SliderCreateRequest request) {
        SliderEntity slider = sliderMapper.toEntity(request);
        slider = sliderRepository.save(slider);
        return sliderMapper.toResponse(slider);
    }

    @Override
    @Transactional
    public SliderResponse updateSlider(Long sliderId, SliderUpdateRequest request) {
        SliderEntity slider = sliderRepository.findById(sliderId)
                .orElseThrow(() -> new AppException(ErrorCode.SLIDER_NOT_FOUND));

        sliderMapper.updateEntityFromRequest(request, slider);
        slider = sliderRepository.save(slider);
        return sliderMapper.toResponse(slider);
    }

    @Override
    @Transactional
    public void deleteSlider(Long sliderId) {
        SliderEntity slider = sliderRepository.findById(sliderId)
                .orElseThrow(() -> new AppException(ErrorCode.SLIDER_NOT_FOUND));
        
        sliderRepository.delete(slider);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SliderResponse> getAllSliders(Pageable pageable) {
        return sliderRepository.findAll(pageable)
                .map(sliderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SliderResponse> getActiveSliders() {
        List<SliderEntity> sliders = sliderRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return sliderMapper.toResponseList(sliders);
    }
}
