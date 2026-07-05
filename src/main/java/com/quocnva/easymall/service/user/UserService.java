package com.quocnva.easymall.service.user;

import com.quocnva.easymall.dtos.request.user.CreateUserRequest;
import com.quocnva.easymall.dtos.request.user.UpdateUserRequest;
import com.quocnva.easymall.dtos.response.user.UserDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User management service interface.
 * Encapsulates: get current user, update profile, admin user CRUD operations.
 */
public interface UserService {

    UserDetailResponse getCurrentUser();

    Page<UserDetailResponse> getAllUsers(Pageable pageable);

    UserDetailResponse getUserById(Long id);

    UserDetailResponse createUser(CreateUserRequest request);

    UserDetailResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
