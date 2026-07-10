package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.slider.SliderCreateRequest;
import com.quocnva.easymall.dtos.request.slider.SliderUpdateRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.slider.SliderResponse;
import com.quocnva.easymall.service.slider.SliderService;
import com.quocnva.easymall.util.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sliders")
@RequiredArgsConstructor
public class SliderController {

    private final SliderService sliderService;

    // --- Public APIs ---

    @GetMapping("/public")
    public ApiResponse<List<SliderResponse>> getActiveSliders() {
        return ApiResponse.<List<SliderResponse>>builder()
                .result(sliderService.getActiveSliders())
                .build();
    }

    // --- Admin/Private APIs ---

    @GetMapping
    @PreAuthorize("@permissionChecker.has('slider:read')")
    public ApiResponse<Page<SliderResponse>> getAllSliders(Pageable pageable) {
        return ApiResponse.<Page<SliderResponse>>builder()
                .result(sliderService.getAllSliders(pageable))
                .build();
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.has('slider:create')")
    public ApiResponse<SliderResponse> createSlider(@Valid @RequestBody SliderCreateRequest request) {
        return ApiResponse.<SliderResponse>builder()
                .result(sliderService.createSlider(request))
                .build();
    }

    @PutMapping("/{sliderId}")
    @PreAuthorize("@permissionChecker.has('slider:update')")
    public ApiResponse<SliderResponse> updateSlider(
            @PathVariable Long sliderId,
            @Valid @RequestBody SliderUpdateRequest request) {
        return ApiResponse.<SliderResponse>builder()
                .result(sliderService.updateSlider(sliderId, request))
                .build();
    }

    @DeleteMapping("/{sliderId}")
    @PreAuthorize("@permissionChecker.has('slider:delete')")
    public ApiResponse<Void> deleteSlider(@PathVariable Long sliderId) {
        sliderService.deleteSlider(sliderId);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.slider.deleted"))
                .build();
    }
}
