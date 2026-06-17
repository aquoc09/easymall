package com.quocnva.easymall.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class AddressController {
}
