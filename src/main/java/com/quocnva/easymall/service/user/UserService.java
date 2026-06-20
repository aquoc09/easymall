package com.quocnva.easymall.service.user;

import com.quocnva.easymall.dtos.response.auth.UserResponse;

/**
 * User management service interface.
 * Encapsulates: get current user, update profile, admin user operations.
 */
public interface UserService {

    UserResponse getCurrentUser();
}
