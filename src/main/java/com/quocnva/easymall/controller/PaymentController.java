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
            boolean success = vnPayService.handleIpn(request);
            if (success) {
                return new VnPayIpnResponse("00", "Confirm Success");
            } else {
                return new VnPayIpnResponse("02", "Order already confirmed or Invalid signature");
            }
        } catch (Exception e) {
            return new VnPayIpnResponse("99", "Unknown error");
        }
    }

    @GetMapping("/vnpay/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionStatus = request.getParameter("vnp_TransactionStatus");
        String trackingNumber = request.getParameter("vnp_TxnRef");
        
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        
        String redirectUrl = frontendResultUrl + "?trackingNumber=" + URLEncoder.encode(trackingNumber != null ? trackingNumber : "", StandardCharsets.UTF_8)
                + "&success=" + success;
                
        response.sendRedirect(redirectUrl);
    }
}
