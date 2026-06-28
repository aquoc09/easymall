package com.quocnva.easymall.service.ghn.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quocnva.easymall.config.GhnProperties;
import com.quocnva.easymall.dtos.response.ghn.GhnDistrictResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnProvinceResponse;
import com.quocnva.easymall.dtos.response.ghn.GhnWardResponse;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.service.ghn.GhnMasterDataService;
import com.quocnva.easymall.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnMasterDataServiceImpl implements GhnMasterDataService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final GhnProperties ghnProperties;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ── Province ──────────────────────────────────────────────────────────────

    @Override
    public List<GhnProvinceResponse> getProvinces() {
        String key = RedisKeyUtil.ghnProvincesKey();
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }

        String url = ghnProperties.getUrl() + "/master-data/province";
        List<GhnProvinceResponse> result = callGhn(url, null, new TypeReference<>() {});

        cache(key, result);
        return result;
    }

    // ── District ──────────────────────────────────────────────────────────────

    @Override
    public List<GhnDistrictResponse> getDistricts(Integer provinceId) {
        String key = RedisKeyUtil.ghnDistrictsKey(provinceId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }

        String url = ghnProperties.getUrl() + "/master-data/district";
        Map<String, Object> body = Map.of("province_id", provinceId);
        List<GhnDistrictResponse> result = callGhn(url, body, new TypeReference<>() {});

        cache(key, result);
        return result;
    }

    // ── Ward ──────────────────────────────────────────────────────────────────

    @Override
    public List<GhnWardResponse> getWards(Integer districtId) {
        String key = RedisKeyUtil.ghnWardsKey(districtId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return deserialize(cached, new TypeReference<>() {});
        }

        String url = ghnProperties.getUrl() + "/master-data/ward";
        Map<String, Object> body = Map.of("district_id", districtId);
        List<GhnWardResponse> result = callGhn(url, body, new TypeReference<>() {});

        cache(key, result);
        return result;
    }

    // ── Ward → District lookup ────────────────────────────────────────────────

    @Override
    public Integer getDistrictIdByWardCode(String wardCode, Integer districtId) {
        // Dùng districtId để lấy list wards (đã cached), tìm ward khớp wardCode
        return getWards(districtId).stream()
                .filter(w -> wardCode.equals(w.getWardCode()))
                .map(GhnWardResponse::getDistrictId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.GHN_INVALID_LOCATION));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Gọi GHN API (GET hoặc POST với body).
     * GHN luôn trả HTTP 200 — phải check code trong JSON body.
     */
    private <T> List<T> callGhn(String url, Object body, TypeReference<List<T>> typeRef) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<?> entity = (body != null)
                ? new HttpEntity<>(body, headers)
                : new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = (body != null)
                    ? restTemplate.exchange(url, HttpMethod.POST, entity, String.class)
                    : restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                String message = root.path("message").asText("GHN error");
                log.error("[GHN] API error code={} message={}", code, message);
                throw new AppException(ErrorCode.GHN_INVALID_LOCATION);
            }

            return objectMapper.convertValue(root.path("data"), typeRef);

        } catch (RestClientException e) {
            log.error("[GHN] Connection error to {}: {}", url, e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        } catch (JsonProcessingException e) {
            log.error("[GHN] JSON parse error: {}", e.getMessage());
            throw new AppException(ErrorCode.GHN_INTEGRATION_ERROR);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken());
        return headers;
    }

    private <T> void cache(String key, T data) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("[GHN] Could not serialize to Redis cache for key={}: {}", key, e.getMessage());
        }
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("[GHN] Cache parse error, will re-fetch: {}", e.getMessage());
            return null;
        }
    }
}
