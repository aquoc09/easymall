package com.quocnva.easymall.controller;

import com.quocnva.easymall.dtos.response.payment.VnPayIpnResponse;
import com.quocnva.easymall.service.payment.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VnPayService vnPayService;

    @Value("${vn-pay.frontend-result-url:http://localhost:5173/payment-result}")
    private String frontendResultUrl;

    @GetMapping("/vnpay/ipn")
    public VnPayIpnResponse vnpayIpn(HttpServletRequest request) {
        try {
            int result = vnPayService.processVnPayCallback(request);
            if (result == 1) {
                return new VnPayIpnResponse("00", "Confirm Success");
            } else if (result == 2) {
                return new VnPayIpnResponse("02", "Order already confirmed");
            } else if (result == 0) {
                return new VnPayIpnResponse("97", "Invalid signature");
            } else {
                return new VnPayIpnResponse("04", "Invalid amount or Order not found");
            }
        } catch (Exception e) {
            return new VnPayIpnResponse("99", "Unknown error");
        }
    }

    @GetMapping("/vnpay/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String trackingNumber = request.getParameter("vnp_TxnRef");
        
        // Gọi processVnPayCallback để verify chữ ký và update DB (phòng trường hợp IPN chưa kịp chạy)
        int result = vnPayService.processVnPayCallback(request);
        
        // Kết quả = 1 (vừa update thành công) hoặc 2 (đã update trước đó) thì xem như success
        boolean success = (result == 1 || result == 2);
        
        String redirectUrl = frontendResultUrl + "?trackingNumber=" + URLEncoder.encode(trackingNumber != null ? trackingNumber : "", StandardCharsets.UTF_8)
                + "&success=" + success;
                
        response.sendRedirect(redirectUrl);
    }
}
