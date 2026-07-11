package com.quocnva.easymall.service.ghn.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quocnva.easymall.config.GhnProperties;
import com.quocnva.easymall.dtos.request.ghn.ShippingFeeRequest;
import com.quocnva.easymall.dtos.response.ghn.GhnServiceResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnShippingFeeResponse;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.service.ghn.GhnShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnShippingServiceImpl implements GhnShippingService {

    private final GhnProperties ghnProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<GhnServiceResponse> getAvailableServices(Integer toDistrictId) {
        Integer fromDistrictId = ghnProperties.getDistrictId();

        Map<String, Object> body = Map.of(
                "shop_id", ghnProperties.getShopId(),
                "from_district", fromDistrictId,
                "to_district", toDistrictId
        );

        return callGhn(
                ghnProperties.getUrl() + "/v2/shipping-order/available-services",
                body,
                new TypeReference<>() {}
        );
    }

    @Override
    public GhnShippingFeeResponse calculateFee(ShippingFeeRequest request) {
        Integer fromDistrictId = ghnProperties.getDistrictId();

        // Bước 1: Lấy danh sách service hợp lệ cho tuyến đường
        List<GhnServiceResponse> services = getAvailableServices(request.getToDistrictId());
        if (services.isEmpty()) {
            throw new AppException(ErrorCode.GHN_SERVICE_UNAVAILABLE);
        }

        Integer serviceId = request.getServiceId();
        String shortName = "";

        if (serviceId != null) {
            // Nếu có truyền serviceId, kiểm tra xem có hợp lệ cho tuyến đường này không
            GhnServiceResponse matchedService = services.stream()
                    .filter(s -> s.getServiceId().equals(request.getServiceId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.GHN_SERVICE_UNAVAILABLE));
            shortName = matchedService.getShortName();
        } else {
            // Nếu không truyền, mặc định lấy service đầu tiên (thường là rẻ/chuẩn nhất)
            serviceId = services.get(0).getServiceId();
            shortName = services.get(0).getShortName();
        }

        // Bước 2: Tính phí
        Map<String, Object> body = new HashMap<>();
        body.put("service_id", serviceId);
        body.put("shop_id", ghnProperties.getShopId());
        body.put("from_district_id", fromDistrictId);
        body.put("from_ward_code", ghnProperties.getWardCode());
        body.put("to_district_id", request.getToDistrictId());
        body.put("to_ward_code", request.getToWardCode());
        body.put("weight", request.getWeightGram());
        body.put("insurance_value", 0);

        try {
            HttpHeaders headers = buildHeaders();
            headers.set("ShopId", String.valueOf(ghnProperties.getShopId()));
            ResponseEntity<String> response = restTemplate.exchange(
                    ghnProperties.getUrl() + "/v2/shipping-order/fee",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            checkGhnCode(root);

            JsonNode data = root.path("data");
            int finalServiceId = serviceId;
            String finalShortName = shortName;
            return GhnShippingFeeResponse.builder()
                    .total(data.path("total").asInt())
                    .serviceFee(data.path("service_fee").asInt())
                    .insuranceFee(data.path("insurance_fee").asInt())
                    .serviceId(finalServiceId)
                    .shortName(finalShortName)
                    .build();

        } catch (RestClientException e) {
            log.error("[GHN] calculateFee connection error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        } catch (JsonProcessingException e) {
            log.error("[GHN] calculateFee JSON parse error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> List<T> callGhn(String url, Object body, TypeReference<List<T>> typeRef) {
        try {
            HttpEntity<?> entity = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            checkGhnCode(root);
            return objectMapper.convertValue(root.path("data"), typeRef);

        } catch (RestClientException e) {
            log.error("[GHN] Connection error to {}: {}", url, e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        } catch (JsonProcessingException e) {
            log.error("[GHN] JSON parse error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        }
    }

    private void checkGhnCode(JsonNode root) {
        int code = root.path("code").asInt(-1);
        if (code != 200) {
            String msg = root.path("message").asText();
            log.error("[GHN] API error code={} msg={}", code, msg);
            if (msg.toLowerCase().contains("not supported") || msg.toLowerCase().contains("service")) {
                throw new AppException(ErrorCode.GHN_SERVICE_UNAVAILABLE);
            }
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken());
        return headers;
    }
}
