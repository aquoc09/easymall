package com.quocnva.easymall.service.risk;

import com.quocnva.easymall.entity.OrderEntity;
import com.quocnva.easymall.entity.RiskAlertEntity;
import com.quocnva.easymall.entity.RiskRuleConfigEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.enums.OrderStatus;
import com.quocnva.easymall.event.OrderCreatedEvent;
import com.quocnva.easymall.repository.DeviceSessionRepository;
import com.quocnva.easymall.repository.OrderRepository;
import com.quocnva.easymall.repository.RiskAlertRepository;
import com.quocnva.easymall.repository.RiskRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleEngine {

    private final RiskRuleConfigRepository riskRuleConfigRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final OrderRepository orderRepository;
    private final DeviceSessionRepository deviceSessionRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void evaluateOrderRisk(OrderCreatedEvent event) {
        log.info("RiskRuleEngine evaluating order: {}", event.getOrderId());

        OrderEntity order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) return;

        UserEntity user = order.getUser();
        List<RiskRuleConfigEntity> activeRules = riskRuleConfigRepository.findByIsActiveTrue();

        for (RiskRuleConfigEntity rule : activeRules) {
            boolean isViolated = false;
            String description = "";

            OffsetDateTime windowStart = rule.getTimeWindowMinutes() != null ?
                    OffsetDateTime.now().minus(rule.getTimeWindowMinutes(), ChronoUnit.MINUTES) :
                    null;

            switch (rule.getRuleCode()) {
                case "R1_MULTIPLE_DEVICES":
                    if (windowStart != null) {
                        long distinctDevices = deviceSessionRepository.countDistinctDevicesByUserSince(user.getUserId(), windowStart);
                        if (distinctDevices >= rule.getThresholdValue().longValue()) {
                            isViolated = true;
                            description = "User đăng nhập từ " + distinctDevices + " thiết bị khác nhau trong " + rule.getTimeWindowMinutes() + " phút.";
                        }
                    }
                    break;
                case "R2_FAILED_PAYMENTS":
                    if (windowStart != null) {
                        long failedOrders = orderRepository.countOrdersByUserAndStatusSince(user.getUserId(), OrderStatus.CANCELLED, windowStart);
                        if (failedOrders >= rule.getThresholdValue().longValue()) {
                            isViolated = true;
                            description = "User có " + failedOrders + " đơn hàng (hoặc thanh toán) thất bại (CANCELLED) trong " + rule.getTimeWindowMinutes() + " phút.";
                        }
                    }
                    break;
                case "R5_NEW_ACC_HIGH_VALUE":
                    OffsetDateTime accountCreated = user.getCreatedAt();
                    if (accountCreated != null && accountCreated.isAfter(OffsetDateTime.now().minus(7, ChronoUnit.DAYS))) {
                        if (order.getFinalPaymentMoney().compareTo(rule.getThresholdValue()) >= 0) {
                            isViolated = true;
                            description = "Tài khoản mới đăng ký (dưới 7 ngày) đặt đơn hàng giá trị cao: " + order.getFinalPaymentMoney() + " VNĐ.";
                        }
                    }
                    break;
                default:
                    // Log unknown rules
                    log.debug("Unknown rule code: {}", rule.getRuleCode());
                    break;
            }

            if (isViolated) {
                // Tạo Alert
                RiskAlertEntity alert = RiskAlertEntity.builder()
                        .user(user)
                        .order(order)
                        .ruleConfig(rule)
                        .description(description)
                        .status("PENDING")
                        .build();
                riskAlertRepository.save(alert);
                log.warn("Risk Alert Generated: Order ID {} violated rule {}", order.getOrderId(), rule.getRuleCode());
            }
        }
    }
}
