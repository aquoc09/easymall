package com.quocnva.easymall.service.contact;

import com.quocnva.easymall.dtos.request.contact.ContactMessageRequest;
import com.quocnva.easymall.dtos.request.contact.ContactMessageStatusRequest;
import com.quocnva.easymall.dtos.response.contact.ContactMessageResponse;
import com.quocnva.easymall.entity.ContactMessageEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.ContactMessageMapper;
import com.quocnva.easymall.repository.ContactMessageRepository;
import com.quocnva.easymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;
    private final ContactMessageMapper contactMessageMapper;

    @Override
    @Transactional
    public ContactMessageResponse createMessage(ContactMessageRequest request, String userEmail) {
        UserEntity user = null;
        if (StringUtils.hasText(userEmail)) {
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        } else {
            // Guest must provide name and email
            if (!StringUtils.hasText(request.getGuestName()) || !StringUtils.hasText(request.getGuestEmail())) {
                // Here we can throw a generic bad request or we can reuse some error. 
                // Using AppException with a generic uncategorized but we can just throw IllegalArgumentException handled by GlobalExceptionHandler
                throw new IllegalArgumentException("Guest name and email are required when not logged in");
            }
        }

        ContactMessageEntity entity = ContactMessageEntity.builder()
                .user(user)
                .guestName(user != null ? user.getFullName() : request.getGuestName())
                .guestEmail(user != null ? user.getEmail() : request.getGuestEmail())
                .subject(request.getSubject())
                .content(request.getContent())
                .status("PENDING")
                .build();

        entity = contactMessageRepository.save(entity);
        return contactMessageMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getMyMessages(String userEmail, Pageable pageable) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return contactMessageRepository.findByUser_UserId(user.getUserId(), pageable)
                .map(contactMessageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getAdminMessages(String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            return contactMessageRepository.findByStatus(status, pageable)
                    .map(contactMessageMapper::toResponse);
        }
        return contactMessageRepository.findAll(pageable)
                .map(contactMessageMapper::toResponse);
    }

    @Override
    @Transactional
    public ContactMessageResponse updateStatus(Long messageId, ContactMessageStatusRequest request) {
        ContactMessageEntity entity = contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!"PENDING".equals(entity.getStatus())) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        entity.setStatus(request.getStatus());
        entity = contactMessageRepository.save(entity);
        return contactMessageMapper.toResponse(entity);
    }
}
