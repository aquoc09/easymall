package com.quocnva.easymall.service.tracking;

import com.quocnva.easymall.dtos.request.tracking.TrackingEventRequest;

public interface TrackingService {
    void trackEvent(TrackingEventRequest request);
}
