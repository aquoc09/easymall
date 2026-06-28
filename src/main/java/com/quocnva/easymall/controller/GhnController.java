package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.ghn.GhnWebhookRequest;
import com.quocnva.easymall.dtos.request.ghn.ShippingFeeRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.ghn.*;
import com.quocnva.easymall.service.ghn.GhnMasterDataService;
import com.quocnva.easymall.service.ghn.GhnShippingService;
import com.quocnva.easymall.service.ghn.GhnWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GHN integration endpoints:
 *
 * Master data (public):
 *   GET  /api/v1/ghn/provinces
 *   GET  /api/v1/ghn/districts?provinceId={id}
 *   GET  /api/v1/ghn/wards?districtId={id}
 *
 * Shipping (user):
 *   POST /api/v1/ghn/shipping-fee
 *
 * Webhook (public — GHN callback):
 *   POST /api/v1/ghn/webhook
 */
@RestController
@RequestMapping("/api/v1/ghn")
@RequiredArgsConstructor
public class GhnController {

    private final GhnMasterDataService ghnMasterDataService;
    private final GhnShippingService ghnShippingService;
    private final GhnWebhookService ghnWebhookService;

    // ── Master Data (public — frontend cần khi mở form địa chỉ) ──────────

    @GetMapping("/provinces")
    public ApiResponse<List<GhnProvinceResponse>> getProvinces() {
        return ApiResponse.<List<GhnProvinceResponse>>builder()
                .result(ghnMasterDataService.getProvinces())
                .build();
    }

    @GetMapping("/districts")
    public ApiResponse<List<GhnDistrictResponse>> getDistricts(
            @RequestParam Integer provinceId) {
        return ApiResponse.<List<GhnDistrictResponse>>builder()
                .result(ghnMasterDataService.getDistricts(provinceId))
                .build();
    }

    @GetMapping("/wards")
    public ApiResponse<List<GhnWardResponse>> getWards(
            @RequestParam Integer districtId) {
        return ApiResponse.<List<GhnWardResponse>>builder()
                .result(ghnMasterDataService.getWards(districtId))
                .build();
    }

    // ── Shipping Fee (user) ───────────────────────────────────────────────

    @PostMapping("/shipping-fee")
    @PreAuthorize("@permissionChecker.has('ghn:read')")
    public ApiResponse<GhnShippingFeeResponse> calculateFee(
            @Valid @RequestBody ShippingFeeRequest request) {
        return ApiResponse.<GhnShippingFeeResponse>builder()
                .result(ghnShippingService.calculateFee(request))
                .build();
    }

    // ── Webhook (public — GHN gọi về, không có user token) ───────────────
    // Security: validate bằng header Token trong GhnWebhookServiceImpl

    @PostMapping("/webhook")
    public ApiResponse<Void> handleWebhook(@RequestBody GhnWebhookRequest request) {
        ghnWebhookService.handleWebhook(request);
        return ApiResponse.<Void>builder().build();
    }
}
