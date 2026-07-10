package com.quocnva.easymall.mapper;

import com.quocnva.easymall.dtos.response.contact.ContactMessageResponse;
import com.quocnva.easymall.entity.ContactMessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContactMessageMapper {

    @Mapping(source = "user.userId", target = "userId")
    ContactMessageResponse toResponse(ContactMessageEntity entity);
}
