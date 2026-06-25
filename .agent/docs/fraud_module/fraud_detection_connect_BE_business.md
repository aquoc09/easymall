# BE kết nối với API Module Phát hiện gian lận

### **PHẦN 1: TÀI LIỆU ĐẶC TẢ API (API CONTRACT)**

_Bạn có thể copy phần này bỏ vào file Word/Markdown báo cáo luận văn mục "Thiết kế Giao tiếp Hệ thống"._

- **Giao thức:** HTTP POST
- **Đường dẫn (Endpoint):** http://localhost:8000/api/orders/checkout
- **Mục đích:** Gửi thông tin giao dịch sang Module AI để nhận về điểm rủi ro và các yếu tố cảnh báo.

**1. Cấu trúc dữ liệu gửi đi (Request Payload - JSON):**

```json
{
  "order_total_amount": 250000.0,
  "payment_method": 1,
  "is_vpn_proxy": 0,
  "location_mismatch": 0,
  "orders_per_device_24h": 1,
  "account_age_days": 30,
  "reputation_score": 100.0,
  "failed_payment_attempts_10m": 0,
  "total_distinct_devices": 1,
  "return_rate": 0.05
}
```

````

**2. Cấu trúc dữ liệu nhận về (Response Payload - JSON):**

```json
{
  "risk_score": 99.88,
  "top_risk_factors": [
    "Tỷ lệ hoàn trả hàng trong quá khứ rất cao",
    "Thanh toán lỗi nhiều lần liên tiếp"
  ]
}
````

---

### **PHẦN 2: MÃ NGUỒN TÍCH HỢP BÊN JAVA SPRING BOOT**

Trong project Spring Boot của bạn, hãy tạo 3 file sau để gọi API bên Python. Cách phổ biến và dễ giải thích nhất cho sinh viên là sử dụng RestTemplate.

#### **1. Tạo file FraudRequestDTO.java (Gói dữ liệu gửi đi)**

Lưu ý: Java dùng quy tắc camelCase (chữ hoa xen kẽ), còn Python dùng snake_case (dấu gạch dưới). Ta dùng @JsonProperty để map chúng lại với nhau chuẩn xác.

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class FraudRequestDTO {

    @JsonProperty("order_total_amount")
    private Double orderTotalAmount;
    @JsonProperty("payment_method")
    private Integer paymentMethod;
    @JsonProperty("is_vpn_proxy")
    private Integer isVpnProxy;
    @JsonProperty("location_mismatch")
    private Integer locationMismatch;
    @JsonProperty("orders_per_device_24h")
    private Integer ordersPerDevice24h;
    @JsonProperty("account_age_days")
    private Integer accountAgeDays;
    @JsonProperty("reputation_score")
    private Double reputationScore;
    @JsonProperty("failed_payment_attempts_10m")
    private Integer failedPaymentAttempts10m;
    @JsonProperty("total_distinct_devices")
    private Integer totalDistinctDevices;
    @JsonProperty("return_rate")
    private Double returnRate;
}
```

#### **2. Tạo file FraudResponseDTO.java (Gói dữ liệu nhận về)**

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class FraudResponseDTO {

    @JsonProperty("risk_score")
    private Double riskScore;
    @JsonProperty("top_risk_factors")
    private List<String> topRiskFactors;
}
```

#### **3. Tạo file AiIntegrationService.java (Thực hiện cuộc gọi qua mạng)**

Đây là Service chịu trách nhiệm lấy cục data từ Spring Boot bắn sang cổng 8000 của FastAPI.

```java
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
public class AiIntegrationService {
    // Đường dẫn tới Server Python của bạn
    private final String AI_SERVER_URL = "http://localhost:8000/api/orders/checkout";

    // Spring Boot tự động tiêm RestTemplate vào
    private final RestTemplate restTemplate;

    public AiIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FraudResponseDTO checkTransactionRisk(FraudRequestDTO requestData) {
        try {
            // 1. Cấu hình Header là JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Đóng gói dữ liệu gửi đi
            HttpEntity<FraudRequestDTO> entity = new HttpEntity<>(requestData, headers);

            // 3. Gọi POST request sang Server Python và hứng kết quả vào FraudResponseDTO
            ResponseEntity<FraudResponseDTO> response = restTemplate.postForEntity(
                AI_SERVER_URL,
                entity,
                FraudResponseDTO.class
            );
            return response.getBody();

        } catch (Exception e) {
            // Xử lý lỗi nếu Server AI bị sập hoặc timeout
            System.err.println("Lỗi kết nối đến AI Server: " + e.getMessage());

            // Trả về mặc định an toàn (hoặc ném Exception tùy logic của bạn)
            FraudResponseDTO fallbackResponse = new FraudResponseDTO();
            fallbackResponse.setRiskScore(0.0);
            return fallbackResponse;
        }
    }
}
```
