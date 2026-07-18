package com.quocnva.easymall.service.risk.impl;

import com.quocnva.easymall.dtos.request.risk.RiskAlertResolveRequest;
import com.quocnva.easymall.dtos.request.risk.RiskRuleUpdateRequest;
import com.quocnva.easymall.dtos.response.risk.RiskAlertResponse;
import com.quocnva.easymall.dtos.response.risk.RiskRuleResponse;
import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.entity.RiskAlertEntity;
import com.quocnva.easymall.entity.RiskRuleConfigEntity;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.repository.OrderRepository;
import com.quocnva.easymall.repository.RiskAlertRepository;
import com.quocnva.easymall.repository.RiskRuleConfigRepository;
import com.quocnva.easymall.service.risk.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskRuleConfigRepository riskRuleConfigRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RiskRuleResponse> getAllRules() {
        return riskRuleConfigRepository.findAll().stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RiskRuleResponse updateRule(String ruleCode, RiskRuleUpdateRequest request) {
        RiskRuleConfigEntity rule = riskRuleConfigRepository.findById(ruleCode)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // TODO: Add specific ErrorCode if needed

        rule.setThresholdValue(request.getThresholdValue());
        rule.setTimeWindowMinutes(request.getTimeWindowMinutes());
        rule.setIsActive(request.getIsActive());

        RiskRuleConfigEntity saved = riskRuleConfigRepository.save(rule);
        return mapToRuleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RiskAlertResponse> getAlerts(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return riskAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::mapToAlertResponse);
        }
        return riskAlertRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToAlertResponse);
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, RiskAlertResolveRequest request) {
        RiskAlertEntity alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        String status = request.getStatus();
        if (!"RESOLVED".equals(status) && !"FALSE_POSITIVE".equals(status)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        alert.setStatus(status);
        riskAlertRepository.save(alert);

        if (alert.getOrder() != null) {
            OrderEntity order = alert.getOrder();
            if ("RESOLVED".equals(status)) {
                // If it is actual fraud, cancel order
                if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);
                }
            }
        }
    }

    private RiskRuleResponse mapToRuleResponse(RiskRuleConfigEntity entity) {
        return RiskRuleResponse.builder()
                .ruleCode(entity.getRuleCode())
                .ruleName(entity.getRuleName())
                .riskLevel(entity.getRiskLevel())
                .thresholdValue(entity.getThresholdValue())
                .timeWindowMinutes(entity.getTimeWindowMinutes())
                .isActive(entity.getIsActive())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private RiskAlertResponse mapToAlertResponse(RiskAlertEntity entity) {
        return RiskAlertResponse.builder()
                .alertId(entity.getAlertId())
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .orderId(entity.getOrder() != null ? entity.getOrder().getOrderId() : null)
                .ruleCode(entity.getRuleConfig() != null ? entity.getRuleConfig().getRuleCode() : null)
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
