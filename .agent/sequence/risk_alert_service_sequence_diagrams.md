# Sequence Diagrams for Risk Alert Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `RiskServiceImpl` và `RiskRuleEngine` bất đồng bộ.

## 1. Quản lý Quy tắc (`getAllRules`, `updateRule`)

Luồng này minh họa cách admin cập nhật một Quy tắc Rủi ro (Risk Rule).

```mermaid
sequenceDiagram
    participant Client
    participant RiskService
    participant RiskRuleConfigRepository

    Client->>RiskService: updateRule(ruleCode, RiskRuleUpdateRequest)
    activate RiskService

    RiskService->>RiskRuleConfigRepository: findById(ruleCode)
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskService: RiskRuleConfigEntity (hoặc ném ra NOT_FOUND)
    deactivate RiskRuleConfigRepository

    RiskService->>RiskService: Cập nhật threshold, timeWindow, isActive

    RiskService->>RiskRuleConfigRepository: save(rule)
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskService: savedRule
    deactivate RiskRuleConfigRepository

    RiskService->>RiskService: Ánh xạ thành RiskRuleResponse

    RiskService-->>Client: RiskRuleResponse
    deactivate RiskService
```

## 2. Quản lý Cảnh báo (`getAlerts`, `resolveAlert`)

Luồng này cho thấy cách admin giải quyết một cảnh báo gian lận (ví dụ: xác nhận đó là gian lận hoặc đánh dấu nó là dương tính giả).

```mermaid
sequenceDiagram
    participant Client
    participant RiskService
    participant RiskAlertRepository
    participant OrderRepository

    Client->>RiskService: resolveAlert(alertId, RiskAlertResolveRequest)
    activate RiskService

    RiskService->>RiskAlertRepository: findById(alertId)
    activate RiskAlertRepository
    RiskAlertRepository-->>RiskService: RiskAlertEntity (hoặc ném ra NOT_FOUND)
    deactivate RiskAlertRepository

    alt trạng thái không thuộc [RESOLVED, FALSE_POSITIVE]
        RiskService-->>Client: throw AppException(INVALID_REQUEST)
    end

    RiskService->>RiskService: Cập nhật trạng thái cảnh báo
    
    RiskService->>RiskAlertRepository: save(alert)
    activate RiskAlertRepository
    RiskAlertRepository-->>RiskService: savedAlert
    deactivate RiskAlertRepository

    alt trạng thái == RESOLVED (Gian lận thực sự)
        alt alert.order != null và order.status != CANCELLED
            RiskService->>RiskService: Đặt trạng thái order = CANCELLED
            RiskService->>OrderRepository: save(order)
            activate OrderRepository
            OrderRepository-->>RiskService: savedOrder
            deactivate OrderRepository
        end
    end

    RiskService-->>Client: void
    deactivate RiskService
```

## 3. Công cụ đánh giá rủi ro (`evaluateOrderRisk`)

Luồng này mô tả công việc ngầm bất đồng bộ tự động đánh giá các đơn hàng dựa trên các quy tắc rủi ro đang hoạt động ngay sau khi chúng được tạo.

```mermaid
sequenceDiagram
    participant SystemEvent
    participant RiskRuleEngine
    participant OrderRepository
    participant RiskRuleConfigRepository
    participant DeviceSessionRepository
    participant RiskAlertRepository

    SystemEvent->>RiskRuleEngine: OrderCreatedEvent(orderId)
    Note over RiskRuleEngine: Chạy bất đồng bộ (@Async) sau khi transaction commit
    activate RiskRuleEngine

    RiskRuleEngine->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>RiskRuleEngine: OrderEntity (Thông tin người dùng & thanh toán)
    deactivate OrderRepository

    RiskRuleEngine->>RiskRuleConfigRepository: findByIsActiveTrue()
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskRuleEngine: List<RiskRuleConfigEntity> (các quy tắc đang hoạt động)
    deactivate RiskRuleConfigRepository

    loop Đối với mỗi quy tắc đang hoạt động
        alt ruleCode == R1_MULTIPLE_DEVICES
            RiskRuleEngine->>DeviceSessionRepository: countDistinctDevicesByUserSince(userId, windowStart)
            DeviceSessionRepository-->>RiskRuleEngine: deviceCount
            RiskRuleEngine->>RiskRuleEngine: Kiểm tra nếu deviceCount >= threshold
        else ruleCode == R2_FAILED_PAYMENTS
            RiskRuleEngine->>OrderRepository: countOrdersByUserAndStatusSince(CANCELLED, windowStart)
            OrderRepository-->>RiskRuleEngine: failedCount
            RiskRuleEngine->>RiskRuleEngine: Kiểm tra nếu failedCount >= threshold
        else ruleCode == R5_NEW_ACC_HIGH_VALUE
            RiskRuleEngine->>RiskRuleEngine: Kiểm tra nếu người dùng < 7 ngày tuổi VÀ giá trị đơn hàng >= threshold
        end

        alt isViolated == true
            RiskRuleEngine->>RiskRuleEngine: Xây dựng RiskAlertEntity (Trạng thái: PENDING)
            RiskRuleEngine->>RiskAlertRepository: save(alert)
            activate RiskAlertRepository
            RiskAlertRepository-->>RiskRuleEngine: savedAlert
            deactivate RiskAlertRepository
        end
    end

    deactivate RiskRuleEngine
```
