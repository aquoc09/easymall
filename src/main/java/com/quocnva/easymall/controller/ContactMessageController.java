package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.contact.ContactMessageRequest;
import com.quocnva.easymall.dtos.request.contact.ContactMessageStatusRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.contact.ContactMessageResponse;
import com.quocnva.easymall.service.contact.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/contacts")
    public ApiResponse<ContactMessageResponse> createContactMessage(
            @Valid @RequestBody ContactMessageRequest request,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ContactMessageResponse response = contactMessageService.createMessage(request, userEmail);
        return ApiResponse.<ContactMessageResponse>builder().result(response).build();
    }

    @GetMapping("/contacts/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ContactMessageResponse>> getMyContactMessages(
            Authentication authentication,
            Pageable pageable) {
        String userEmail = authentication.getName();
        Page<ContactMessageResponse> response = contactMessageService.getMyMessages(userEmail, pageable);
        return ApiResponse.<Page<ContactMessageResponse>>builder().result(response).build();
    }

    @GetMapping("/admin/contacts")
    @PreAuthorize("@permissionChecker.has('contact:read')")
    public ApiResponse<Page<ContactMessageResponse>> getAdminContactMessages(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<ContactMessageResponse> response = contactMessageService.getAdminMessages(status, pageable);
        return ApiResponse.<Page<ContactMessageResponse>>builder().result(response).build();
    }

    @PatchMapping("/admin/contacts/{messageId}/status")
    @PreAuthorize("@permissionChecker.has('contact:update')")
    public ApiResponse<ContactMessageResponse> updateContactStatus(
            @PathVariable Long messageId,
            @Valid @RequestBody ContactMessageStatusRequest request) {
        ContactMessageResponse response = contactMessageService.updateStatus(messageId, request);
        return ApiResponse.<ContactMessageResponse>builder().result(response).build();
    }
}
