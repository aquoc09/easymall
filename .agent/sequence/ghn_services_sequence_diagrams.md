# Sequence Diagrams for GHN Services

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `GhnOrderServiceImpl`, `GhnShippingServiceImpl`, và `GhnWebhookServiceImpl`.

## 1. GhnOrderServiceImpl

### 1.1. Tạo Đơn hàng (`createOrder`)

```mermaid
sequenceDiagram
    participant Client
    participant GhnOrderService
    participant ObjectMapper
    participant RestTemplate

    Client->>GhnOrderService: createOrder(OrderEntity)
    activate GhnOrderService

    GhnOrderService->>GhnOrderService: Tính tổng trọng lượng totalWeightGram (tổng của các sản phẩm)
    GhnOrderService->>GhnOrderService: Ánh xạ ShippingMethod thành service_type_id
    GhnOrderService->>GhnOrderService: Tính codAmount
    GhnOrderService->>GhnOrderService: Xây dựng JSON payload và headers

    GhnOrderService->>RestTemplate: exchange("/v2/shipping-order/create", POST)
    activate RestTemplate
    alt RestClientException
        RestTemplate-->>GhnOrderService: Exception
        GhnOrderService-->>Client: throw AppException(GHN_INTEGRATION_ERROR)
    else Phản hồi thành công
        RestTemplate-->>GhnOrderService: Chuỗi JSON phản hồi
    end
    deactivate RestTemplate

    GhnOrderService->>ObjectMapper: readTree(responseJson)
    ObjectMapper-->>GhnOrderService: JsonNode

    alt code != 200
        GhnOrderService-->>Client: throw AppException(GHN_INTEGRATION_ERROR)
    else code == 200
        GhnOrderService->>GhnOrderService: Trích xuất order_code, total_fee, expected_delivery_time
        GhnOrderService-->>Client: GhnCreateOrderResponse
    end
    deactivate GhnOrderService
```

---

## 2. GhnShippingServiceImpl

### 2.1. Lấy các dịch vụ khả dụng (`getAvailableServices`)

```mermaid
sequenceDiagram
    participant Client
    participant GhnShippingService
    participant RestTemplate
    participant ObjectMapper

    Client->>GhnShippingService: getAvailableServices(toDistrictId)
    activate GhnShippingService

    GhnShippingService->>GhnShippingService: Xây dựng payload (shop_id, from_district, to_district)

    GhnShippingService->>RestTemplate: exchange("/v2/shipping-order/available-services", POST)
    activate RestTemplate
    RestTemplate-->>GhnShippingService: Chuỗi JSON phản hồi
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

### 2.2. Tính Phí (`calculateFee`)

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

    alt danh sách dịch vụ trống
        GhnShippingService-->>Client: throw AppException(GHN_SERVICE_UNAVAILABLE)
    end

    alt request.serviceId được cung cấp
        GhnShippingService->>GhnShippingService: Tìm dịch vụ khớp
        alt không tìm thấy
            GhnShippingService-->>Client: throw AppException(GHN_SERVICE_UNAVAILABLE)
        end
    else request.serviceId là null
        GhnShippingService->>GhnShippingService: Chọn dịch vụ đầu tiên
    end

    GhnShippingService->>GhnShippingService: Xây dựng payload

    GhnShippingService->>RestTemplate: exchange("/v2/shipping-order/fee", POST)
    activate RestTemplate
    RestTemplate-->>GhnShippingService: Chuỗi JSON phản hồi
    deactivate RestTemplate

    GhnShippingService->>ObjectMapper: readTree(responseJson)
    ObjectMapper-->>GhnShippingService: JsonNode

    GhnShippingService->>GhnShippingService: checkGhnCode(root)

    GhnShippingService->>GhnShippingService: Trích xuất total, service_fee, insurance_fee
    
    GhnShippingService-->>Client: GhnShippingFeeResponse
    deactivate GhnShippingService
```

---

## 3. GhnWebhookServiceImpl

### 3.1. Xử lý Webhook (`handleWebhook`)

```mermaid
sequenceDiagram
    participant GHN_System
    participant GhnWebhookService
    participant OrderRepository

    GHN_System->>GhnWebhookService: handleWebhook(GhnWebhookRequest)
    activate GhnWebhookService

    GhnWebhookService->>OrderRepository: findByTrackingNumber(request.orderCode)
    activate OrderRepository
    alt Không tìm thấy đơn hàng
        OrderRepository-->>GhnWebhookService: Optional.empty()
        GhnWebhookService-->>GHN_System: trả về (Bỏ qua)
    else Đã tìm thấy đơn hàng
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
