package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.address.CreateAddressRequest;
import com.quocnva.easymall.dtos.request.address.UpdateAddressRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.order.AddressResponse;
import com.quocnva.easymall.service.address.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles address management:
 * GET    /api/v1/addresses
 * POST   /api/v1/addresses
 * PUT    /api/v1/addresses/{id}
 * DELETE /api/v1/addresses/{id}
 * PATCH  /api/v1/addresses/{id}/default
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // ── GET my addresses ───────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("@permissionChecker.has('address:manage')")
    public ApiResponse<List<AddressResponse>> getMyAddresses(Authentication auth) {
        return ApiResponse.<List<AddressResponse>>builder()
                .result(addressService.getMyAddresses(auth.getName()))
                .build();
    }

    // ── CREATE ─────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionChecker.has('address:manage')")
    public ApiResponse<AddressResponse> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            Authentication auth) {
        return ApiResponse.<AddressResponse>builder()
                .result(addressService.createAddress(request, auth.getName()))
                .build();
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('address:manage')")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request,
            Authentication auth) {
        return ApiResponse.<AddressResponse>builder()
                .result(addressService.updateAddress(id, request, auth.getName()))
                .build();
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.has('address:manage')")
    public ApiResponse<Void> deleteAddress(
            @PathVariable Long id,
            Authentication auth) {
        addressService.deleteAddress(id, auth.getName());
        return ApiResponse.<Void>builder().build();
    }

    // ── SET DEFAULT ────────────────────────────────────────────────────────

    @PatchMapping("/{id}/default")
    @PreAuthorize("@permissionChecker.has('address:manage')")
    public ApiResponse<AddressResponse> setDefault(
            @PathVariable Long id,
            Authentication auth) {
        return ApiResponse.<AddressResponse>builder()
                .result(addressService.setDefault(id, auth.getName()))
                .build();
    }
}
