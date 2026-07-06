package com.quocnva.easymall.service.fraud.impl;


import com.quocnva.easymall.dtos.request.order.FraudRequestDTO;
import com.quocnva.easymall.dtos.response.order.FraudResponseDTO;
import com.quocnva.easymall.entity.*;
import com.quocnva.easymall.enums.SystemDecision;
import com.quocnva.easymall.repository.FraudRecordRepository;
import com.quocnva.easymall.repository.FraudRuleConfigRepository;
import com.quocnva.easymall.repository.UserStatsRepository;
import com.quocnva.easymall.service.fraud.AiIntegrationService;
import com.quocnva.easymall.service.fraud.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final AiIntegrationService aiIntegrationService;
    private final UserStatsRepository userStatsRepository;
    private final FraudRuleConfigRepository fraudRuleConfigRepository;
    private final FraudRecordRepository fraudRecordRepository;

    @Override
    @Transactional
    public SystemDecision evaluateOrder(OrderEntity order, DeviceSessionEntity deviceSession, UserEntity user) {
        
        // 1. Feature Extraction
        UserStatsEntity stats = userStatsRepository.findById(user.getUserId()).orElse(null);
        
        int accountAgeDays = 0;
        double reputationScore = 100.0;
        int failedPaymentAttempts = 0;
        int totalDistinctDevices = 1;
        double returnRate = 0.0;

        if (stats != null) {
            accountAgeDays = stats.getAccountAgeDays() != null ? stats.getAccountAgeDays() : 0;
            reputationScore = stats.getReputationScore() != null ? stats.getReputationScore() : 100.0;
            failedPaymentAttempts = stats.getFailedPaymentAttempts10m() != null ? stats.getFailedPaymentAttempts10m() : 0;
            totalDistinctDevices = stats.getTotalDistinctDevices() != null ? stats.getTotalDistinctDevices() : 1;
            
            int totalOrders = stats.getTotalOrders() != null ? stats.getTotalOrders() : 0;
            int returnedOrders = stats.getReturnedOrdersCount() != null ? stats.getReturnedOrdersCount() : 0;
            if (totalOrders > 0) {
                returnRate = (double) returnedOrders / totalOrders;
            }
        }

        int paymentMethodCode = order.getPaymentMethod() != null ? order.getPaymentMethod().ordinal() : 0;
        int isVpnProxy = (deviceSession != null && Boolean.TRUE.equals(deviceSession.getIsVpnProxy())) ? 1 : 0;
        
        // Tạm thời mock logic location_mismatch = 0 cho tới session tiếp theo
        int locationMismatch = 0;
        
        // TODO [Fraud Detection]: device_session_id đã bị xoá khỏi OrderEntity.
        // Cần implement lại orders_per_device_24h qua user_id hoặc IP address.
        // Tạm thời mock = 1 để không ảnh hưởng fraud score.
        int ordersPerDevice24h = 1;


        FraudRequestDTO request = FraudRequestDTO.builder()
                .orderTotalAmount(order.getFinalPaymentMoney() != null ? order.getFinalPaymentMoney().doubleValue() : 0.0)
                .paymentMethod(paymentMethodCode)
                .isVpnProxy(isVpnProxy)
                .locationMismatch(locationMismatch)
                .ordersPerDevice24h(ordersPerDevice24h)
                .accountAgeDays(accountAgeDays)
                .reputationScore(reputationScore)
                .failedPaymentAttempts10m(failedPaymentAttempts)
                .totalDistinctDevices(totalDistinctDevices)
                .returnRate(returnRate)
                .build();

        // 2. Call ML Service
        FraudResponseDTO aiResponse = aiIntegrationService.checkTransactionRisk(request);
        Double riskScore = aiResponse.getRiskScore() != null ? aiResponse.getRiskScore() : 0.0;

        // 3. Get Rules from Cache/DB
        FraudRuleConfigEntity ruleConfig = getActiveFraudRuleConfig();
        double reviewThreshold = ruleConfig != null ? ruleConfig.getReviewThreshold() : 40.0;
        double declineThreshold = ruleConfig != null ? ruleConfig.getDeclineThreshold() : 75.0;

        // 4. Decision Engine
        SystemDecision decision = SystemDecision.APPROVE;
        if (riskScore > declineThreshold) {
            decision = SystemDecision.DECLINE;
        } else if (riskScore > reviewThreshold) {
            decision = SystemDecision.REVIEW;
        }

        // 5. Update user restriction if declined
        if (decision == SystemDecision.DECLINE && stats != null) {
            stats.setIsRestricted(true);
            userStatsRepository.save(stats);
        }

        // 6. Record Fraud Decision
        saveFraudRecord(order, aiResponse, decision);
        
        return decision;
    }

    private void saveFraudRecord(OrderEntity savedOrder, FraudResponseDTO aiResponse, SystemDecision decision) {
        FraudRecordEntity record = FraudRecordEntity.builder()
                .order(savedOrder)
                .riskScore(aiResponse.getRiskScore() != null ? aiResponse.getRiskScore() : 0.0)
                .systemDecision(decision)
                .finalLabel("PENDING")
                .topRiskFactors(aiResponse.getTopRiskFactors())
                .build();

        fraudRecordRepository.save(record);
    }

    @Cacheable(value = "fraud_rule_configs", key = "'active_config'")
    public FraudRuleConfigEntity getActiveFraudRuleConfig() {
        return fraudRuleConfigRepository.findById(1).orElse(null);
    }
}
