package com.quocnva.easymall.service.fraud;

import com.quocnva.easymall.dtos.request.order.FraudRequestDTO;
import com.quocnva.easymall.dtos.response.order.FraudResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private static final String AI_SERVER_URL = "http://localhost:8000/api/ml/fraud/predict";
    private final RestTemplate restTemplate;

    public FraudResponseDTO checkTransactionRisk(FraudRequestDTO requestData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<FraudRequestDTO> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<FraudResponseDTO> response = restTemplate.postForEntity(
                    AI_SERVER_URL,
                    entity,
                    FraudResponseDTO.class
            );
            return response.getBody();

        } catch (Exception e) {
            log.error("[AI_FALLBACK] Lỗi kết nối đến AI Server: {}", e.getMessage());
            // Chiến lược Fail-Open
            FraudResponseDTO fallbackResponse = new FraudResponseDTO();
            fallbackResponse.setRiskScore(0.0);
            return fallbackResponse;
        }
    }
}
