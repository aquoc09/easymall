package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.request.tracking.TrackingEventRequest;
import com.quocnva.easymall.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/events")
    public ResponseEntity<Void> trackEvent(@Valid @RequestBody TrackingEventRequest request) {
        // Asynchronously process the tracking event
        trackingService.trackEvent(request);
        // Return 202 Accepted immediately
        return ResponseEntity.accepted().build();
    }
}
