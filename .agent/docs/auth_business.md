# 🔐 Auth Business — Luồng hoạt động Authentication & Authorization

**Project**: EasyMall  
**Last Updated**: 2026-06-21  
**Base URL**: `/api/v1/auth`

---

## 1. Tổng quan kiến trúc

### 1.1 Service Layer (Package-by-Feature)

```
service/auth/
├── RegistrationService         → Đăng ký + kích hoạt tài khoản + gửi lại OTP
├── AuthenticationService       → Đăng nhập + Đăng xuất
├── TokenService                → Sinh AT/RT, refresh token, introspect
├── PasswordResetService        → Quên mật khẩu + đặt lại mật khẩu
service/user/
├── UserService                 → Lấy thông tin user hiện tại (/me)
service/email/
├── EmailService                → Gửi email OTP (ACTIVATION / FORGOT_PASSWORD)
```

### 1.2 Component Diagram

```mermaid
graph TB
    Client["HTTP Client"]

    subgraph "Controller Layer"
        AC["AuthController<br>/api/v1/auth"]
    end

    subgraph "Service Layer"
        RS["RegistrationService"]
        AS["AuthenticationService"]
        TS["TokenService"]
        PS["PasswordResetService"]
        US["UserService"]
        ES["EmailService"]
    end

    subgraph "Infrastructure"
        DB[(PostgreSQL)]
        RD[(Redis)]
        MAIL["SMTP Server"]
    end

    Client --> AC
    AC --> RS
    AC --> AS
    AC --> TS
    AC --> PS
    AC --> US

    RS --> ES
    PS --> ES
    AS --> TS

    RS --> DB
    RS --> RD
    AS --> DB
    AS --> RD
    TS --> DB
    TS --> RD
    PS --> DB
    PS --> RD
    US --> DB
    ES --> MAIL
```

### 1.3 Security Architecture

| Component | Vai trò |
|:--|:--|
| `SecurityConfig` | Cấu hình Spring Security, CSRF disabled, Stateless session |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` — extract Bearer token, validate, set SecurityContext |
| `JwtUtil` | Sinh/parse JWT (HMAC-SHA256 via Nimbus JOSE) |
| `JwtConfig` | `@ConfigurationProperties(prefix = "jwt")` — signerKey, validDuration, refreshableDuration |
| `ApplicationInitConfig` | `ApplicationRunner` — seed ROLE_ADMIN, ROLE_USER + admin account on startup |

**Public endpoints** (không cần token):
```
POST /api/v1/auth/register
POST /api/v1/auth/active
POST /api/v1/auth/resend-otp
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/refresh
POST /api/v1/auth/introspect
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

**Protected endpoints**: Tất cả endpoint khác yêu cầu `Authorization: Bearer <access_token>`.

---

## 2. Luồng hoạt động chi tiết

### 2.1 Registration Flow (2-step)

> **Đặc điểm**: Không ghi DB cho đến khi OTP được xác thực → tránh tạo tài khoản rác.

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant RS as RegistrationService
    participant RD as Redis
    participant ES as EmailService
    participant DB as PostgreSQL

    Note over C,DB: STEP 1 — POST /register
    C->>AC: RegisterRequest {fullName, email, password, phone}
    AC->>RS: register(request)
    RS->>DB: existsByEmail(email)?
    alt Email đã tồn tại
        RS-->>C: 409 EMAIL_ALREADY_EXISTS
    end
    RS->>RD: hasKey(otp_rate:ACTIVATION:{email})?
    alt Rate limited
        RS-->>C: 429 OTP_ALREADY_SENT
    end
    RS->>RS: Encode password (BCrypt)
    RS->>RS: Generate OTP (6 digits)
    RS->>RD: SET pending_user:{email} = JSON{fullName, email, password, phone} TTL=300s
    RS->>RD: SET otp:ACTIVATION:{email} = OTP  TTL=300s
    RS->>RD: SET otp_rate:ACTIVATION:{email} = ""  TTL=60s
    RS->>ES: sendOtpEmail(email, otp, ACTIVATION)
    ES->>ES: Send email via SMTP
    RS-->>AC: void
    AC-->>C: 200 {message: "success.register"}

    Note over C,DB: STEP 2 — POST /active
    C->>AC: ActivateAccountRequest {email, otp}
    AC->>RS: activateAccount(request)
    RS->>RD: GET pending_user:{email}
    alt Không tìm thấy pending data
        RS-->>C: 404 PENDING_REGISTRATION_NOT_FOUND
    end
    RS->>RD: GET otp:ACTIVATION:{email}
    alt OTP sai hoặc hết hạn
        RS-->>C: 400 OTP_INVALID
    end
    RS->>RS: Deserialize JSON → Map
    RS->>DB: findByRoleName("ROLE_USER")
    RS->>DB: save(UserEntity) ← @Transactional
    RS->>RD: DELETE pending_user:{email}
    RS->>RD: DELETE otp:ACTIVATION:{email}
    RS-->>AC: void
    AC-->>C: 200 {message: "success.activate-account"}
```

#### Redis Keys sử dụng

| Key Pattern | TTL | Mục đích |
|:--|:--|:--|
| `pending_user:{email}` | 300s | Lưu trữ tạm thông tin user chưa xác thực |
| `otp:ACTIVATION:{email}` | 300s | Mã OTP 6 chữ số |
| `otp_rate:ACTIVATION:{email}` | 60s | Rate-limit sentinel — 1 OTP / 60s / email |

---

### 2.2 Login Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthenticationService
    participant TS as TokenService
    participant DB as PostgreSQL

    C->>AC: LoginRequest {email, password}
    AC->>AS: login(request)
    AS->>DB: findByEmail(email)
    alt User không tồn tại
        AS-->>C: 404 USER_NOT_FOUND
    end
    AS->>AS: Check isActive == true
    alt Tài khoản bị vô hiệu hóa
        AS-->>C: 403 ACCOUNT_NOT_ACTIVE
    end
    AS->>AS: passwordEncoder.matches(rawPwd, encodedPwd)
    alt Sai mật khẩu
        AS-->>C: 401 INVALID_CREDENTIALS
    end
    AS->>TS: generateAndSaveTokens(user, deviceInfo=null)
    TS->>TS: JwtUtil.generateAccessToken(user)
    TS->>TS: JwtUtil.generateRefreshToken(user)
    TS->>DB: save(TokenEntity{refreshToken, expiresAt, user})
    TS-->>AS: AuthResponse{accessToken, refreshToken}
    AS-->>AC: AuthResponse
    AC-->>C: 200 {result: {accessToken, refreshToken}}
```

#### JWT Claims Structure

**Access Token:**
```json
{
  "sub": "user@email.com",
  "jti": "uuid",
  "iss": "easymall",
  "iat": 1719000000,
  "exp": 1719000900,
  "scope": "ROLE_USER",
  "type": "ACCESS"
}
```

**Refresh Token:**
```json
{
  "sub": "user@email.com",
  "jti": "uuid",
  "iss": "easymall",
  "iat": 1719000000,
  "exp": 1719604800,
  "type": "REFRESH"
}
```

---

### 2.3 Logout Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthenticationService
    participant RD as Redis
    participant DB as PostgreSQL

    C->>AC: LogoutRequest {accessToken, refreshToken}
    AC->>AS: logout(request)

    Note over AS: Blacklist Access Token
    AS->>AS: JwtUtil.getJti(accessToken) → jti
    AS->>AS: JwtUtil.getRemainingTtlSeconds(accessToken) → ttl
    alt ttl > 0
        AS->>RD: SET blacklist:at:{jti} = ""  TTL=remaining_seconds
    end

    Note over AS: Delete Refresh Token
    AS->>DB: deleteByRefreshToken(refreshToken)

    AS-->>AC: void
    AC-->>C: 200 {message: "success.logout"}
```

> **Chiến lược**: AT bị blacklist trong Redis (auto-expire khi AT hết hạn). RT bị xóa row khỏi DB → không thể refresh.

---

### 2.4 Refresh Token Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant TS as TokenService
    participant DB as PostgreSQL

    C->>AC: RefreshTokenRequest {token}
    AC->>TS: refresh(request)
    TS->>TS: JwtUtil.parseToken(token) — verify signature
    alt Signature invalid
        TS-->>C: 400 REFRESH_TOKEN_INVALID
    end
    TS->>DB: findByRefreshToken(token)
    alt Không tìm thấy RT
        TS-->>C: 400 REFRESH_TOKEN_INVALID
    end
    TS->>TS: Check expiresAt > now()
    alt RT hết hạn
        TS->>DB: delete(tokenEntity)
        TS-->>C: 400 REFRESH_TOKEN_INVALID
    end

    Note over TS: Token Rotation
    TS->>DB: delete(old TokenEntity)
    TS->>TS: generateAndSaveTokens(user, deviceInfo)
    TS->>DB: save(new TokenEntity)
    TS-->>AC: AuthResponse{newAccessToken, newRefreshToken}
    AC-->>C: 200 {result: {accessToken, refreshToken}}
```

> **Token Rotation**: Mỗi lần refresh → RT cũ bị xóa, RT mới được tạo. Ngăn chặn replay attack.

---

### 2.5 Token Introspect

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant TS as TokenService
    participant RD as Redis

    C->>AC: IntrospectRequest {token}
    AC->>TS: introspect(request)
    TS->>TS: JwtUtil.parseToken(token) — verify signature + expiry
    alt Parse thất bại
        TS-->>C: 200 {result: {valid: false}}
    end
    TS->>RD: hasKey(blacklist:at:{jti})
    alt Đã bị blacklist
        TS-->>C: 200 {result: {valid: false}}
    end
    TS-->>AC: IntrospectResponse{valid: true}
    AC-->>C: 200 {result: {valid: true}}
```

---

### 2.6 Password Reset Flow (2-step)

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant PS as PasswordResetService
    participant RD as Redis
    participant ES as EmailService
    participant DB as PostgreSQL

    Note over C,DB: STEP 1 — POST /forgot-password
    C->>AC: ForgotPasswordRequest {email}
    AC->>PS: forgotPassword(request, clientIp)
    PS->>DB: findByEmail(email)
    alt User không tồn tại
        PS-->>C: 404 USER_NOT_FOUND
    end
    PS->>RD: hasKey(otp_rate:FORGOT_PASSWORD:{email})?
    alt Rate limited
        PS-->>C: 429 OTP_ALREADY_SENT
    end
    PS->>PS: Generate OTP (6 digits)
    PS->>RD: SET otp:FORGOT_PASSWORD:{email} = OTP  TTL=300s
    PS->>RD: SET otp_rate:FORGOT_PASSWORD:{email} = ""  TTL=60s
    PS->>ES: sendOtpEmail(email, otp, FORGOT_PASSWORD)
    PS-->>AC: void
    AC-->>C: 200 {message: "success.forgot-password"}

    Note over C,DB: STEP 2 — POST /reset-password
    C->>AC: ResetPasswordRequest {email, otp, newPassword}
    AC->>PS: resetPassword(request)
    PS->>RD: GET otp:FORGOT_PASSWORD:{email}
    alt OTP sai hoặc hết hạn
        PS-->>C: 400 OTP_INVALID
    end
    PS->>DB: findByEmail(email)
    PS->>PS: BCrypt encode(newPassword)
    PS->>DB: save(user) ← @Transactional
    PS->>RD: DELETE otp:FORGOT_PASSWORD:{email}
    PS-->>AC: void
    AC-->>C: 200 {message: "success.reset-password"}
```

---

### 2.7 Resend OTP Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant RS as RegistrationService
    participant RD as Redis
    participant ES as EmailService

    C->>AC: ResendOtpRequest {email, type: ACTIVATION|FORGOT_PASSWORD}
    AC->>RS: resendOtp(request, clientIp)
    RS->>RD: hasKey(otp_rate:{TYPE}:{email})?
    alt Rate limited (< 60s since last send)
        RS-->>C: 429 OTP_ALREADY_SENT
    end
    RS->>RS: Generate new OTP
    RS->>RD: SET otp:{TYPE}:{email} = OTP  TTL=300s
    RS->>RD: SET otp_rate:{TYPE}:{email} = ""  TTL=60s
    RS->>ES: sendOtpEmail(email, otp, type)
    RS-->>AC: void
    AC-->>C: 200 {message: "success.resend-otp"}
```

---

### 2.8 Get Current User (/me)

```mermaid
sequenceDiagram
    participant C as Client
    participant Filter as JwtAuthenticationFilter
    participant AC as AuthController
    participant US as UserService
    participant DB as PostgreSQL

    C->>Filter: GET /api/v1/auth/me (Authorization: Bearer AT)
    Filter->>Filter: Extract & parse AT
    Filter->>Filter: Verify: signature ✓ + expiry ✓ + not blacklisted ✓
    Filter->>Filter: Set SecurityContext(email, authorities)
    Filter->>AC: request proceeds
    AC->>US: getCurrentUser()
    US->>US: SecurityContextHolder.getContext().getAuthentication().getName()
    US->>DB: findByEmail(email)
    US->>US: Map UserEntity → UserResponse
    US-->>AC: UserResponse
    AC-->>C: 200 {result: {userId, email, fullName, roleName, ...}}
```

---

## 3. JWT Authentication Pipeline

### 3.1 Request Lifecycle

```mermaid
flowchart TB
    A["HTTP Request arrives"] --> B{"Has Authorization header?"}
    B -- No --> C["Continue unauthenticated"]
    B -- Yes --> D["JwtAuthenticationFilter"]
    D --> E["Extract Bearer token"]
    E --> F["JwtUtil.parseToken() — verify HMAC-SHA256"]
    F -- Invalid --> C
    F -- Valid --> G{"Token expired?"}
    G -- Yes --> C
    G -- No --> H{"Blacklisted in Redis?"}
    H -- Yes --> C
    H -- No --> I["Set SecurityContext<br>principal=email, authorities=scope"]
    I --> J["Continue to Controller"]
    C --> K{"Endpoint requires auth?"}
    K -- No --> L["Process request"]
    K -- Yes --> M["403 Forbidden"]
```

### 3.2 Token Storage Strategy

| Token | Storage | Revocation |
|:------|:--------|:-----------|
| **Access Token (AT)** | Client-side only (không lưu DB) | Blacklist JTI trong Redis, TTL = remaining lifetime |
| **Refresh Token (RT)** | DB table `tokens` (full JWT string) | Delete row from DB |

---

## 4. Redis Key Map

| Key Pattern | TTL | Type | Mô tả |
|:--|:--|:--|:--|
| `pending_user:{email}` | 300s | String (JSON) | Pending registration data — chưa verify OTP |
| `otp:ACTIVATION:{email}` | 300s | String | OTP 6 chữ số cho đăng ký |
| `otp:FORGOT_PASSWORD:{email}` | 300s | String | OTP 6 chữ số cho quên mật khẩu |
| `otp_rate:ACTIVATION:{email}` | 60s | String (empty) | Rate-limit sentinel cho register/resend |
| `otp_rate:FORGOT_PASSWORD:{email}` | 60s | String (empty) | Rate-limit sentinel cho forgot-password |
| `blacklist:at:{jti}` | AT remaining TTL | String (empty) | Blacklisted Access Token (sau logout) |

---

## 5. API Endpoints Summary

| Method | Endpoint | Request DTO | Response DTO | Service | Auth |
|:-------|:---------|:------------|:-------------|:--------|:-----|
| `POST` | `/register` | `RegisterRequest` | `Void` | RegistrationService | ❌ |
| `POST` | `/active` | `ActivateAccountRequest` | `Void` | RegistrationService | ❌ |
| `POST` | `/resend-otp` | `ResendOtpRequest` | `Void` | RegistrationService | ❌ |
| `POST` | `/login` | `LoginRequest` | `AuthResponse` | AuthenticationService | ❌ |
| `POST` | `/logout` | `LogoutRequest` | `Void` | AuthenticationService | ❌ |
| `POST` | `/refresh` | `RefreshTokenRequest` | `AuthResponse` | TokenService | ❌ |
| `POST` | `/introspect` | `IntrospectRequest` | `IntrospectResponse` | TokenService | ❌ |
| `POST` | `/forgot-password` | `ForgotPasswordRequest` | `Void` | PasswordResetService | ❌ |
| `POST` | `/reset-password` | `ResetPasswordRequest` | `Void` | PasswordResetService | ❌ |
| `GET` | `/me` | — | `UserResponse` | UserService | ✅ |

> Tất cả response đều wrapped trong `ApiResponse<T>` với format: `{code, message, result}`.

---

## 6. Data Model

### 6.1 Entity Relationship

```mermaid
erDiagram
    users ||--o{ tokens : "has many"
    users }o--|| roles : "belongs to"

    users {
        BIGINT user_id PK
        VARCHAR email UK
        VARCHAR password
        VARCHAR full_name
        SMALLINT gender
        VARCHAR phone
        DATE dob
        VARCHAR facebook_account_id
        VARCHAR google_account_id
        BOOLEAN is_active
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
        BIGINT role_id FK
    }

    roles {
        BIGINT role_id PK
        VARCHAR role_name UK
    }

    tokens {
        BIGINT token_id PK
        VARCHAR refresh_token
        BOOLEAN is_revoked
        TIMESTAMPTZ expires_at
        VARCHAR device_info
        BIGINT user_id FK
    }
```

### 6.2 Default Data (ApplicationInitConfig)

| Entity | Data | Tạo bởi |
|:-------|:-----|:--------|
| Role | `ROLE_ADMIN` | `ApplicationRunner` on startup |
| Role | `ROLE_USER` | `ApplicationRunner` on startup |
| User | `admin@easymall.com` / `admin@123` / ROLE_ADMIN | `ApplicationRunner` on startup |

---

## 7. Configuration

### 7.1 JWT Config (`application.yaml`)

```yaml
jwt:
  signer-key: ${JWT_SIGNER_KEY}        # HMAC-SHA256 secret key
  valid-duration: 900000               # Access Token TTL: 15 minutes (ms)
  refreshable-duration: 604800000      # Refresh Token TTL: 7 days (ms)
```

### 7.2 OTP Constants

| Constant | Value | Mô tả |
|:---------|:------|:------|
| `OTP_TTL_SECONDS` | 300 (5 min) | Thời gian sống của OTP |
| `OTP_RATE_TTL_SECONDS` | 60 (1 min) | Cooldown giữa các lần gửi OTP |
| `RT_TTL_DAYS` | 7 | Refresh Token lifetime (days) |

---

## 8. Error Codes (Auth-related)

| ErrorCode | HTTP Status | Mô tả |
|:----------|:------------|:------|
| `EMAIL_ALREADY_EXISTS` | 409 | Email đã được đăng ký |
| `OTP_ALREADY_SENT` | 429 | Rate-limited — chờ 60s |
| `PENDING_REGISTRATION_NOT_FOUND` | 404 | Hết hạn hoặc chưa đăng ký |
| `OTP_INVALID` | 400 | OTP sai hoặc hết hạn |
| `USER_NOT_FOUND` | 404 | Không tìm thấy user |
| `ACCOUNT_NOT_ACTIVE` | 403 | Tài khoản bị vô hiệu hóa |
| `INVALID_CREDENTIALS` | 401 | Sai mật khẩu |
| `REFRESH_TOKEN_INVALID` | 400 | RT không hợp lệ hoặc hết hạn |
| `RESOURCE_NOT_FOUND` | 404 | Role không tồn tại |

---

## 9. Internationalization (i18n)

Tất cả message trả về qua `Translator.toLocale(key)` → đọc từ `messages.properties`:

```properties
# Success messages
success.register=Registration OTP sent successfully
success.activate-account=Account activated successfully
success.resend-otp=OTP resent successfully
success.logout=Logged out successfully
success.forgot-password=Password reset OTP sent successfully
success.reset-password=Password reset successfully

# Email templates
email.activation.subject=EasyMall — Activate your account
email.activation.body=Your activation code is: {0}
email.forgot-password.subject=EasyMall — Reset your password
email.forgot-password.body=Your password reset code is: {0}
```

---

## 10. Security Considerations

| Concern | Implementation |
|:--------|:--------------|
| **Password Storage** | BCrypt (via `PasswordEncoder`) |
| **Token Signing** | HMAC-SHA256 (Nimbus JOSE) |
| **CSRF** | Disabled (stateless API) |
| **Session** | Stateless (`SessionCreationPolicy.STATELESS`) |
| **AT Revocation** | Redis blacklist with auto-expiring TTL |
| **RT Revocation** | Row deletion from DB |
| **Token Rotation** | Old RT deleted, new pair generated on refresh |
| **OTP Spam Prevention** | Rate-limit sentinel in Redis (60s cooldown) |
| **No DB Pollution** | Pending users stored in Redis, not DB |
| **Admin Seed** | Idempotent — only creates if not exists |
