package com.quocnva.easymall.service.contact;

import com.quocnva.easymall.dtos.request.contact.ContactMessageRequest;
import com.quocnva.easymall.dtos.request.contact.ContactMessageStatusRequest;
import com.quocnva.easymall.dtos.response.contact.ContactMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactMessageService {
    ContactMessageResponse createMessage(ContactMessageRequest request, String userEmail);

    Page<ContactMessageResponse> getMyMessages(String userEmail, Pageable pageable);

    Page<ContactMessageResponse> getAdminMessages(String status, Pageable pageable);

    ContactMessageResponse updateStatus(Long messageId, ContactMessageStatusRequest request);
}
