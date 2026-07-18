package com.quocnva.easymall.service.payment;

import com.quocnva.easymall.dtos.request.payment.VnPayPaymentRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface VnPayService {
    String createPaymentUrl(VnPayPaymentRequest request) throws Exception;
    boolean handleIpn(HttpServletRequest request);
}
