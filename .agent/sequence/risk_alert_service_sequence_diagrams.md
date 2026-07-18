# Sequence Diagrams for Risk Alert Service

This document contains the sequence diagrams for operations within `RiskServiceImpl` and the asynchronous `RiskRuleEngine`.

## 1. Rule Management (`getAllRules`, `updateRule`)

This flow illustrates how the admin updates a Risk Rule.

```mermaid
sequenceDiagram
    participant Client
    participant RiskService
    participant RiskRuleConfigRepository

    Client->>RiskService: updateRule(ruleCode, RiskRuleUpdateRequest)
    activate RiskService

    RiskService->>RiskRuleConfigRepository: findById(ruleCode)
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskService: RiskRuleConfigEntity (or throw NOT_FOUND)
    deactivate RiskRuleConfigRepository

    RiskService->>RiskService: Update threshold, timeWindow, isActive

    RiskService->>RiskRuleConfigRepository: save(rule)
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskService: savedRule
    deactivate RiskRuleConfigRepository

    RiskService->>RiskService: Map to RiskRuleResponse

    RiskService-->>Client: RiskRuleResponse
    deactivate RiskService
```

## 2. Alert Management (`getAlerts`, `resolveAlert`)

This flow shows how the admin resolves a fraud alert (e.g., confirming it's fraud or marking it as a false positive).

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
    RiskAlertRepository-->>RiskService: RiskAlertEntity (or throw NOT_FOUND)
    deactivate RiskAlertRepository

    alt status not in [RESOLVED, FALSE_POSITIVE]
        RiskService-->>Client: throw AppException(INVALID_REQUEST)
    end

    RiskService->>RiskService: Set alert status
    
    RiskService->>RiskAlertRepository: save(alert)
    activate RiskAlertRepository
    RiskAlertRepository-->>RiskService: savedAlert
    deactivate RiskAlertRepository

    alt status == RESOLVED (Actual Fraud)
        alt alert.order != null and order.status != CANCELLED
            RiskService->>RiskService: Set order status = CANCELLED
            RiskService->>OrderRepository: save(order)
            activate OrderRepository
            OrderRepository-->>RiskService: savedOrder
            deactivate OrderRepository
        end
    end

    RiskService-->>Client: void
    deactivate RiskService
```

## 3. Risk Evaluation Engine (`evaluateOrderRisk`)

This flow describes the automatic, asynchronous background job that evaluates orders against active risk rules immediately after they are created.

```mermaid
sequenceDiagram
    participant SystemEvent
    participant RiskRuleEngine
    participant OrderRepository
    participant RiskRuleConfigRepository
    participant DeviceSessionRepository
    participant RiskAlertRepository

    SystemEvent->>RiskRuleEngine: OrderCreatedEvent(orderId)
    Note over RiskRuleEngine: Runs Asynchronously (@Async) after transaction commit
    activate RiskRuleEngine

    RiskRuleEngine->>OrderRepository: findById(orderId)
    activate OrderRepository
    OrderRepository-->>RiskRuleEngine: OrderEntity (User & Payment info)
    deactivate OrderRepository

    RiskRuleEngine->>RiskRuleConfigRepository: findByIsActiveTrue()
    activate RiskRuleConfigRepository
    RiskRuleConfigRepository-->>RiskRuleEngine: List<RiskRuleConfigEntity> (active rules)
    deactivate RiskRuleConfigRepository

    loop For each active rule
        alt ruleCode == R1_MULTIPLE_DEVICES
            RiskRuleEngine->>DeviceSessionRepository: countDistinctDevicesByUserSince(userId, windowStart)
            DeviceSessionRepository-->>RiskRuleEngine: deviceCount
            RiskRuleEngine->>RiskRuleEngine: Check if deviceCount >= threshold
        else ruleCode == R2_FAILED_PAYMENTS
            RiskRuleEngine->>OrderRepository: countOrdersByUserAndStatusSince(CANCELLED, windowStart)
            OrderRepository-->>RiskRuleEngine: failedCount
            RiskRuleEngine->>RiskRuleEngine: Check if failedCount >= threshold
        else ruleCode == R5_NEW_ACC_HIGH_VALUE
            RiskRuleEngine->>RiskRuleEngine: Check if user is < 7 days old AND order value >= threshold
        end

        alt isViolated == true
            RiskRuleEngine->>RiskRuleEngine: Build RiskAlertEntity (Status: PENDING)
            RiskRuleEngine->>RiskAlertRepository: save(alert)
            activate RiskAlertRepository
            RiskAlertRepository-->>RiskRuleEngine: savedAlert
            deactivate RiskAlertRepository
        end
    end

    deactivate RiskRuleEngine
```
