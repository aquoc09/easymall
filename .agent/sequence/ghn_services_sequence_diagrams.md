# Sequence Diagrams for GHN Services

This document contains the sequence diagrams for operations within `GhnOrderServiceImpl`, `GhnShippingServiceImpl`, and `GhnWebhookServiceImpl`.

## 1. GhnOrderServiceImpl

### 1.1. Create Order (`createOrder`)

```mermaid
sequenceDiagram
    participant Client
    participant GhnOrderService
    participant ObjectMapper
    participant RestTemplate

    Client->>GhnOrderService: createOrder(OrderEntity)
    activate GhnOrderService

    GhnOrderService->>GhnOrderService: Calculate totalWeightGram (sum of items)
    GhnOrderService->>GhnOrderService: Map ShippingMethod to service_type_id
    GhnOrderService->>GhnOrderService: Calculate codAmount
    GhnOrderService->>GhnOrderService: Build JSON payload and headers

    GhnOrderService->>RestTemplate: exchange("/v2/shipping-order/create", POST)
    activate RestTemplate
    alt RestClientException
        RestTemplate-->>GhnOrderService: Exception
        GhnOrderService-->>Client: throw AppException(GHN_INTEGRATION_ERROR)
    else Success response
        RestTemplate-->>GhnOrderService: Response JSON string
    end
    deactivate RestTemplate

    GhnOrderService->>ObjectMapper: readTree(responseJson)
    ObjectMapper-->>GhnOrderService: JsonNode

    alt code != 200
        GhnOrderService-->>Client: throw AppException(GHN_INTEGRATION_ERROR)
    else code == 200
        GhnOrderService->>GhnOrderService: Extract order_code, total_fee, expected_delivery_time
        GhnOrderService-->>Client: GhnCreateOrderResponse
    end
    deactivate GhnOrderService
```

---

## 2. GhnShippingServiceImpl

### 2.1. Get Available Services (`getAvailableServices`)

```mermaid
sequenceDiagram
    participant Client
    participant GhnShippingService
    participant RestTemplate
    participant ObjectMapper

    Client->>GhnShippingService: getAvailableServices(toDistrictId)
    activate GhnShippingService

    GhnShippingService->>GhnShippingService: Build payload (shop_id, from_district, to_district)

    GhnShippingService->>RestTemplate: exchange("/v2/shipping-order/available-services", POST)
    activate RestTemplate
    RestTemplate-->>GhnShippingService: Response JSON string
    deactivate RestTemplate

    GhnShippingService->>ObjectMapper: readTree(responseJson)
    ObjectMapper-->>GhnShippingService: JsonNode

    GhnShippingService->>GhnShippingService: checkGhnCode(root)
    alt code != 200
        GhnShippingService-->>Client: throw AppException(GHN_INTEGRATION_ERROR)
    end

    GhnShippingService->>ObjectMapper: convertValue(data, List<GhnServiceResponse>)
    ObjectMapper-->>GhnShippingService: List<GhnServiceResponse>

    GhnShippingService-->>Client: List<GhnServiceResponse>
    deactivate GhnShippingService
```

### 2.2. Calculate Fee (`calculateFee`)

```mermaid
sequenceDiagram
    participant Client
    participant GhnShippingService
    participant RestTemplate
    participant ObjectMapper

    Client->>GhnShippingService: calculateFee(ShippingFeeRequest)
    activate GhnShippingService

    GhnShippingService->>GhnShippingService: getAvailableServices(toDistrictId)
    GhnShippingService-->>GhnShippingService: List<GhnServiceResponse>

    alt services is empty
        GhnShippingService-->>Client: throw AppException(GHN_SERVICE_UNAVAILABLE)
    end

    alt request.serviceId is provided
        GhnShippingService->>GhnShippingService: Find matching service
        alt not found
            GhnShippingService-->>Client: throw AppException(GHN_SERVICE_UNAVAILABLE)
        end
    else request.serviceId is null
        GhnShippingService->>GhnShippingService: Pick first service
    end

    GhnShippingService->>GhnShippingService: Build payload

    GhnShippingService->>RestTemplate: exchange("/v2/shipping-order/fee", POST)
    activate RestTemplate
    RestTemplate-->>GhnShippingService: Response JSON string
    deactivate RestTemplate

    GhnShippingService->>ObjectMapper: readTree(responseJson)
    ObjectMapper-->>GhnShippingService: JsonNode

    GhnShippingService->>GhnShippingService: checkGhnCode(root)

    GhnShippingService->>GhnShippingService: Extract total, service_fee, insurance_fee
    
    GhnShippingService-->>Client: GhnShippingFeeResponse
    deactivate GhnShippingService
```

---

## 3. GhnWebhookServiceImpl

### 3.1. Handle Webhook (`handleWebhook`)

```mermaid
sequenceDiagram
    participant GHN_System
    participant GhnWebhookService
    participant OrderRepository

    GHN_System->>GhnWebhookService: handleWebhook(GhnWebhookRequest)
    activate GhnWebhookService

    GhnWebhookService->>OrderRepository: findByTrackingNumber(request.orderCode)
    activate OrderRepository
    alt Order not found
        OrderRepository-->>GhnWebhookService: Optional.empty()
        GhnWebhookService-->>GHN_System: return (Ignore)
    else Order found
        OrderRepository-->>GhnWebhookService: OrderEntity
    end
    deactivate OrderRepository

    GhnWebhookService->>GhnWebhookService: mappedStatus = mapGhnStatus(request.status)
    GhnWebhookService->>GhnWebhookService: order.setDeliveryStatus(mappedStatus)

    alt request.status == "delivered"
        GhnWebhookService->>GhnWebhookService: order.setOrderStatus(COMPLETED)
    else request.status == "cancel"
        GhnWebhookService->>GhnWebhookService: order.setOrderStatus(CANCELLED)
    end

    GhnWebhookService->>OrderRepository: save(order)
    activate OrderRepository
    OrderRepository-->>GhnWebhookService: savedOrder
    deactivate OrderRepository

    GhnWebhookService-->>GHN_System: return void
    deactivate GhnWebhookService
```
