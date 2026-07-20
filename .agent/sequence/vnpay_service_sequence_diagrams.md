# Sequence Diagrams for VNPAY Payment Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `VnPayServiceImpl`.

## 1. Tạo URL thanh toán (`createPaymentUrl`)

```mermaid
sequenceDiagram
    participant Client
    participant VnPayService
    participant VnPayProperties

    Client->>VnPayService: createPaymentUrl(request)
    activate VnPayService

    VnPayService->>VnPayProperties: get vnp_TmnCode, vnp_ReturnUrl, vnp_PayUrl, secretKey
    
    VnPayService->>VnPayService: Xây dựng params (Version, Command, Amount, TxnRef, OrderInfo, v.v.)
    VnPayService->>VnPayService: Tính toán CreateDate và ExpireDate (+15 phút)
    VnPayService->>VnPayService: Sắp xếp các trường theo thứ tự bảng chữ cái
    
    VnPayService->>VnPayService: Xây dựng chuỗi truy vấn và hashData
    
    VnPayService->>VnPayService: hmacSHA512(secretKey, hashData)
    VnPayService->>VnPayService: Thêm secureHash vào chuỗi truy vấn
    
    VnPayService-->>Client: Trả về paymentUrl
    deactivate VnPayService
```

## 2. Xử lý IPN Webhook (`handleIpn`)

```mermaid
sequenceDiagram
    participant VNPAYSystem
    participant VnPayService
    participant OrderRepository

    VNPAYSystem->>VnPayService: handleIpn(HttpServletRequest)
    activate VnPayService

    VnPayService->>VnPayService: Trích xuất tất cả các request parameters
    VnPayService->>VnPayService: Xóa vnp_SecureHash và vnp_SecureHashType

    VnPayService->>VnPayService: Sắp xếp các trường và xây dựng hashData
    VnPayService->>VnPayService: signValue = hmacSHA512(secretKey, hashData)

    alt signValue != vnp_SecureHash
        VnPayService-->>VNPAYSystem: trả về false (Chữ ký không hợp lệ)
    end

    alt vnp_TmnCode không khớp
        VnPayService-->>VNPAYSystem: trả về false (TmnCode không hợp lệ)
    end

    alt vnp_ResponseCode là 00 (Thành công)
        VnPayService->>OrderRepository: findByTrackingNumber(vnp_TxnRef)
        activate OrderRepository
        OrderRepository-->>VnPayService: OrderEntity
        deactivate OrderRepository

        alt order hợp lệ VÀ orderStatus là PENDING_PAYMENT
            VnPayService->>VnPayService: order.setOrderStatus(AWAITING_SHIPMENT)
            VnPayService->>OrderRepository: save(order)
            VnPayService-->>VNPAYSystem: trả về true
        end
    end

    VnPayService-->>VNPAYSystem: trả về false
    deactivate VnPayService
```
