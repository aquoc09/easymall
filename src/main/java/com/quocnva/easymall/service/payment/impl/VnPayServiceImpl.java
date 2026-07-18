package com.quocnva.easymall.service.payment.impl;

import com.quocnva.easymall.config.VnPayProperties;
import com.quocnva.easymall.dtos.request.payment.VnPayPaymentRequest;
import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.repository.OrderRepository;
import com.quocnva.easymall.service.payment.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {
    private static final String VNP_VERSION = "2.1.0";
    private final VnPayProperties vnPayProperties;
    private final OrderRepository orderRepository;

    @Override
    public String createPaymentUrl(VnPayPaymentRequest request) throws Exception {
        long amount = request.getAmount() * 100;
        String vnp_TxnRef = request.getTrackingNumber();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", VNP_VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayProperties.getVnp_TmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", vnp_TxnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", vnPayProperties.getVnp_ReturnUrl());
        params.put("vnp_IpAddr", request.getIpAddress());
        params.put("vnp_Locale", request.getLanguage() != null ? request.getLanguage() : "vn");

        if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
            params.put("vnp_BankCode", request.getBankCode());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        params.put("vnp_CreateDate", sdf.format(cld.getTime()));
        
        cld.add(Calendar.MINUTE, 15);
        params.put("vnp_ExpireDate", sdf.format(cld.getTime()));

        // Sort fields
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Iterator<String> it = fieldNames.iterator(); it.hasNext();) {
            String fieldName = it.next();
            String fieldValue = params.get(fieldName);

            hashData.append(fieldName).append("=")
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                    .append("=")
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (it.hasNext()) {
                query.append("&");
                hashData.append("&");
            }
        }

        String secureHash = VnPayProperties.hmacSHA512(vnPayProperties.getSecretKey(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return vnPayProperties.getVnp_PayUrl() + "?" + query.toString();
    }

    @Override
    public boolean handleIpn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> params.put(key, value[0]));

        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext();) {
            String fieldName = it.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (it.hasNext()) {
                    hashData.append("&");
                }
            }
        }

        String signValue = VnPayProperties.hmacSHA512(vnPayProperties.getSecretKey(), hashData.toString());
        
        if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
            log.error("VNPAY IPN INVALID SIGNATURE");
            return false;
        }

        if (!vnPayProperties.getVnp_TmnCode().equals(params.get("vnp_TmnCode"))) {
            log.error("VNPAY IPN INVALID TMNCODE");
            return false;
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            String trackingNumber = params.get("vnp_TxnRef");

            if (trackingNumber == null || trackingNumber.isEmpty()) {
                return false;
            }

            OrderEntity order = orderRepository.findByTrackingNumber(trackingNumber).orElse(null);
            if (order != null && order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
                // Đổi trạng thái sang AWAITING_SHIPMENT và PaymentStatus là PAID
                order.setOrderStatus(OrderStatus.AWAITING_SHIPMENT);
                // order.setPaymentStatus(PaymentStatus.PAID); // nếu OrderEntity có trường này
                orderRepository.save(order);
                return true;
            }
        }

        return false;
    }
}
