package com.quocnva.easymall.service.ghn.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quocnva.easymall.config.GhnProperties;
import com.quocnva.easymall.dtos.response.ghn.GhnCreateOrderResponse;
import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.enums.PaymentMethod;
import com.quocnva.easymall.enums.ShippingMethod;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.service.ghn.GhnOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnOrderServiceImpl implements GhnOrderService {

    private final GhnProperties ghnProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public GhnCreateOrderResponse createOrder(OrderEntity order) {
        var address = order.getAddress();

        // Tính tổng trọng lượng (gram) từ tất cả items
        int totalWeightGram = order.getOrderDetails().stream()
                .mapToInt(detail -> {
                    BigDecimal weightKg = detail.getVariant().getProduct().getWeightKg();
                    if (weightKg == null) return 500; // default 500g nếu không có
                    return weightKg.multiply(BigDecimal.valueOf(1000))
                            .multiply(BigDecimal.valueOf(detail.getNumOfProduct()))
                            .intValue();
                })
                .sum();

        // Map ShippingMethod → GHN service_type_id
        // 2 = Giao hàng tiết kiệm (Standard), 1 = Giao hàng nhanh (Express)
        int serviceTypeId = (order.getShippingMethod() == ShippingMethod.EXPRESS) ? 1 : 2;

        // codAmount: chỉ tính nếu thanh toán COD
        int codAmount = (order.getPaymentMethod() == PaymentMethod.COD)
                ? order.getFinalPaymentMoney().intValue()
                : 0;

        Map<String, Object> body = new HashMap<>();
        body.put("shop_id", ghnProperties.getShopId());
        body.put("from_district_id", null); // GHN tự detect từ shop_id
        body.put("from_ward_code", ghnProperties.getWardCode());
        body.put("to_name", address.getRecipientName());
        body.put("to_phone", address.getPhone());
        body.put("to_address", address.getStreetNumber() != null ? address.getStreetNumber() : "");
        body.put("to_ward_code", address.getWardCode());
        body.put("to_district_id", address.getDistrictId());
        body.put("weight", totalWeightGram);
        body.put("service_type_id", serviceTypeId);
        body.put("payment_type_id", 1); // 1 = shop trả phí
        body.put("required_note", "CHOTHUHANG"); // GHN: cho thử hàng
        body.put("cod_amount", codAmount);
        body.put("note", order.getNote() != null ? order.getNote() : "");
        body.put("items", buildItems(order));

        try {
            HttpHeaders headers = buildHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    ghnProperties.getUrl() + "/v2/shipping-order/create",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                String message = root.path("message").asText();
                log.error("[GHN] createOrder error code={} message={}", code, message);
                throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
            }

            JsonNode data = root.path("data");
            return GhnCreateOrderResponse.builder()
                    .orderCode(data.path("order_code").asText())
                    .totalFee(data.path("total_fee").asInt())
                    .expectedDeliveryTime(data.path("expected_delivery_time").asText(""))
                    .build();

        } catch (RestClientException e) {
            log.error("[GHN] createOrder connection error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        } catch (JsonProcessingException e) {
            log.error("[GHN] createOrder JSON parse error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        }
    }

    private java.util.List<Map<String, Object>> buildItems(OrderEntity order) {
        return order.getOrderDetails().stream()
                .map(detail -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", detail.getVariant().getProduct().getProductName());
                    item.put("quantity", detail.getNumOfProduct());
                    BigDecimal weightKg = detail.getVariant().getProduct().getWeightKg();
                    item.put("weight", weightKg != null
                            ? weightKg.multiply(BigDecimal.valueOf(1000)).intValue()
                            : 500);
                    return item;
                })
                .toList();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken());
        headers.set("ShopId", String.valueOf(ghnProperties.getShopId()));
        return headers;
    }
}
