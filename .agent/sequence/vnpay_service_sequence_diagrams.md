# Sequence Diagrams for VNPAY Payment Service

This document contains the sequence diagrams for operations within `VnPayServiceImpl`.

## 1. Create Payment URL (`createPaymentUrl`)

```mermaid
sequenceDiagram
    participant Client
    participant VnPayService
    participant VnPayProperties

    Client->>VnPayService: createPaymentUrl(request)
    activate VnPayService

    VnPayService->>VnPayProperties: get vnp_TmnCode, vnp_ReturnUrl, vnp_PayUrl, secretKey
    
    VnPayService->>VnPayService: Build params (Version, Command, Amount, TxnRef, OrderInfo, etc.)
    VnPayService->>VnPayService: Calculate CreateDate and ExpireDate (+15 mins)
    VnPayService->>VnPayService: Sort fields alphabetically
    
    VnPayService->>VnPayService: Build query string and hashData
    
    VnPayService->>VnPayService: hmacSHA512(secretKey, hashData)
    VnPayService->>VnPayService: Append secureHash to query string
    
    VnPayService-->>Client: Return paymentUrl
    deactivate VnPayService
```

## 2. Handle IPN Webhook (`handleIpn`)

```mermaid
sequenceDiagram
    participant VNPAYSystem
    participant VnPayService
    participant OrderRepository

    VNPAYSystem->>VnPayService: handleIpn(HttpServletRequest)
    activate VnPayService

    VnPayService->>VnPayService: Extract all request parameters
    VnPayService->>VnPayService: Remove vnp_SecureHash and vnp_SecureHashType

    VnPayService->>VnPayService: Sort fields and build hashData
    VnPayService->>VnPayService: signValue = hmacSHA512(secretKey, hashData)

    alt signValue != vnp_SecureHash
        VnPayService-->>VNPAYSystem: return false (Invalid Signature)
    end

    alt vnp_TmnCode is mismatch
        VnPayService-->>VNPAYSystem: return false (Invalid TmnCode)
    end

    alt vnp_ResponseCode is 00 (Success)
        VnPayService->>OrderRepository: findByTrackingNumber(vnp_TxnRef)
        activate OrderRepository
        OrderRepository-->>VnPayService: OrderEntity
        deactivate OrderRepository

        alt order is valid AND orderStatus is PENDING_PAYMENT
            VnPayService->>VnPayService: order.setOrderStatus(AWAITING_SHIPMENT)
            VnPayService->>OrderRepository: save(order)
            VnPayService-->>VNPAYSystem: return true
        end
    end

    VnPayService-->>VNPAYSystem: return false
    deactivate VnPayService
```
