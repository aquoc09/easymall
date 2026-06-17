package com.quocnva.easymall.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user profile endpoints:
 * GET  /api/v1/users/me
 * PUT  /api/v1/users/me
 * GET  /api/v1/users/{id}      (admin)
 * GET  /api/v1/users           (admin)
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
}
