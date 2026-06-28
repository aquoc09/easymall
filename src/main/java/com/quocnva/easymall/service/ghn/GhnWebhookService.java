package com.quocnva.easymall.service.ghn;

import com.quocnva.easymall.dtos.request.ghn.GhnWebhookRequest;

public interface GhnWebhookService {

    /**
     * Xử lý webhook callback từ GHN.
     * Cập nhật delivery_status và order_status trong orders table.
     *
     * @param request payload từ GHN
     */
    void handleWebhook(GhnWebhookRequest request);
}
