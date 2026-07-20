# Sequence Diagrams for Auth Services

Tài liệu này chứa sơ đồ tuần tự cho các dịch vụ xác thực và ủy quyền, bao gồm `AuthenticationServiceImpl`, `PasswordResetServiceImpl`, `RegistrationServiceImpl`, và `TokenServiceImpl`.

---

## 1. AuthenticationServiceImpl

### 1.1. Đăng nhập (`login`)

```mermaid
sequenceDiagram
    participant Client
    participant AuthenticationService
    participant UserRepository
    participant PasswordEncoder
    participant TokenService

    Client->>AuthenticationService: login(LoginRequest)
    activate AuthenticationService

    AuthenticationService->>UserRepository: findByEmail(request.email)
    activate UserRepository
    alt Không tìm thấy người dùng
        UserRepository-->>AuthenticationService: Optional.empty()
        AuthenticationService-->>Client: throw AppException(USER_NOT_FOUND)
    else Tìm thấy người dùng
        UserRepository-->>AuthenticationService: Optional<UserEntity>
    end
    deactivate UserRepository

    alt Người dùng không hoạt động
        AuthenticationService-->>Client: throw AppException(ACCOUNT_NOT_ACTIVE)
    end

    AuthenticationService->>PasswordEncoder: matches(request.password, user.password)
    activate PasswordEncoder
    alt Mật khẩu không khớp
        PasswordEncoder-->>AuthenticationService: false
        AuthenticationService-->>Client: throw AppException(INVALID_CREDENTIALS)
    else Mật khẩu khớp
        PasswordEncoder-->>AuthenticationService: true
    end
    deactivate PasswordEncoder

    AuthenticationService->>TokenService: generateAndSaveTokens(user, null)
    activate TokenService
    TokenService-->>AuthenticationService: AuthResponse
    deactivate TokenService

    AuthenticationService-->>Client: AuthResponse
    deactivate AuthenticationService
```

### 1.2. Đăng xuất (`logout`)

```mermaid
sequenceDiagram
    participant Client
    participant AuthenticationService
    participant JwtUtil
    participant RedisTemplate
    participant TokenRepository

    Client->>AuthenticationService: logout(LogoutRequest)
    activate AuthenticationService

    AuthenticationService->>JwtUtil: getJti(accessToken)
    JwtUtil-->>AuthenticationService: jti

    AuthenticationService->>JwtUtil: getRemainingTtlSeconds(accessToken)
    JwtUtil-->>AuthenticationService: ttl

    alt ttl > 0
        AuthenticationService->>RedisTemplate: set(blacklistAtKey, "", ttl)
        activate RedisTemplate
        RedisTemplate-->>AuthenticationService: void
        deactivate RedisTemplate
    end

    AuthenticationService->>TokenRepository: deleteByRefreshToken(refreshToken)
    activate TokenRepository
    TokenRepository-->>AuthenticationService: void
    deactivate TokenRepository

    AuthenticationService-->>Client: void
    deactivate AuthenticationService
```

---

## 2. PasswordResetServiceImpl

### 2.1. Quên Mật khẩu (`forgotPassword`)

```mermaid
sequenceDiagram
    participant Client
    participant PasswordResetService
    participant UserRepository
    participant RedisTemplate
    participant EmailService

    Client->>PasswordResetService: forgotPassword(request, clientIp)
    activate PasswordResetService

    PasswordResetService->>UserRepository: findByEmail(request.email)
    UserRepository-->>PasswordResetService: Optional<UserEntity>
    
    PasswordResetService->>PasswordResetService: checkOtpRateLimit()
    activate PasswordResetService
    PasswordResetService->>RedisTemplate: hasKey(rateKey)
    alt Bị giới hạn tốc độ
        RedisTemplate-->>PasswordResetService: true
        PasswordResetService-->>Client: throw AppException(OTP_ALREADY_SENT)
    else Được phép
        RedisTemplate-->>PasswordResetService: false
    end
    deactivate PasswordResetService

    PasswordResetService->>PasswordResetService: generateOtp()
    
    PasswordResetService->>RedisTemplate: set(otpKey, otp, OTP_TTL)
    PasswordResetService->>RedisTemplate: set(rateKey, "", OTP_RATE_TTL)
    
    PasswordResetService->>EmailService: sendOtpEmail(email, otp, FORGOT_PASSWORD)
    activate EmailService
    EmailService-->>PasswordResetService: void
    deactivate EmailService

    PasswordResetService-->>Client: void
    deactivate PasswordResetService
```

### 2.2. Đặt lại Mật khẩu (`resetPassword`)

```mermaid
sequenceDiagram
    participant Client
    participant PasswordResetService
    participant RedisTemplate
    participant UserRepository
    participant PasswordEncoder

    Client->>PasswordResetService: resetPassword(request)
    activate PasswordResetService

    PasswordResetService->>RedisTemplate: get(otpKey)
    RedisTemplate-->>PasswordResetService: storedOtp

    alt storedOtp là null HOẶC storedOtp != request.otp
        PasswordResetService-->>Client: throw AppException(OTP_INVALID)
    end

    PasswordResetService->>UserRepository: findByEmail(request.email)
    UserRepository-->>PasswordResetService: UserEntity

    PasswordResetService->>PasswordEncoder: encode(request.newPassword)
    PasswordEncoder-->>PasswordResetService: encodedPassword
    
    PasswordResetService->>UserRepository: save(user)
    
    PasswordResetService->>RedisTemplate: delete(otpKey)

    PasswordResetService-->>Client: void
    deactivate PasswordResetService
```

---

## 3. RegistrationServiceImpl

### 3.1. Đăng ký (`register`)

```mermaid
sequenceDiagram
    participant Client
    participant RegistrationService
    participant UserRepository
    participant PasswordEncoder
    participant RedisTemplate
    participant EmailService

    Client->>RegistrationService: register(RegisterRequest)
    activate RegistrationService

    RegistrationService->>UserRepository: existsByEmail(request.email)
    alt Email đã tồn tại
        UserRepository-->>RegistrationService: true
        RegistrationService-->>Client: throw AppException(EMAIL_ALREADY_EXISTS)
    end
    
    RegistrationService->>RegistrationService: checkOtpRateLimit()
    
    RegistrationService->>PasswordEncoder: encode(request.password)
    PasswordEncoder-->>RegistrationService: encodedPassword
    
    RegistrationService->>RegistrationService: xây dựng pendingUserMap và tuần tự hóa thành JSON
    RegistrationService->>RegistrationService: generateOtp()

    RegistrationService->>RedisTemplate: set(pendingUserKey, pendingJson, OTP_TTL)
    RegistrationService->>RedisTemplate: set(otpKey, otp, OTP_TTL)
    RegistrationService->>RedisTemplate: set(rateKey, "", OTP_RATE_TTL)
    
    RegistrationService->>EmailService: sendOtpEmail(email, otp, ACTIVATION)

    RegistrationService-->>Client: void
    deactivate RegistrationService
```

### 3.2. Kích hoạt Tài khoản (`activateAccount`)

```mermaid
sequenceDiagram
    participant Client
    participant RegistrationService
    participant RedisTemplate
    participant RoleRepository
    participant UserRepository

    Client->>RegistrationService: activateAccount(ActivateAccountRequest)
    activate RegistrationService

    RegistrationService->>RedisTemplate: get(pendingUserKey)
    alt Không có người dùng chờ kích hoạt
        RedisTemplate-->>RegistrationService: null
        RegistrationService-->>Client: throw AppException(PENDING_REGISTRATION_NOT_FOUND)
    end

    RegistrationService->>RedisTemplate: get(otpKey)
    alt OTP không hợp lệ
        RedisTemplate-->>RegistrationService: null hoặc không khớp
        RegistrationService-->>Client: throw AppException(OTP_INVALID)
    end

    RegistrationService->>RegistrationService: giải tuần tự hóa pendingJson
    
    RegistrationService->>RoleRepository: findByRoleName("ROLE_USER")
    RoleRepository-->>RegistrationService: RoleEntity
    
    RegistrationService->>RegistrationService: xây dựng UserEntity (isActive=true)
    
    RegistrationService->>UserRepository: save(userEntity)
    
    RegistrationService->>RedisTemplate: delete(pendingUserKey)
    RegistrationService->>RedisTemplate: delete(otpKey)

    RegistrationService-->>Client: void
    deactivate RegistrationService
```

### 3.3. Gửi lại OTP (`resendOtp`)

```mermaid
sequenceDiagram
    participant Client
    participant RegistrationService
    participant RedisTemplate
    participant EmailService

    Client->>RegistrationService: resendOtp(request, clientIp)
    activate RegistrationService

    RegistrationService->>RegistrationService: checkOtpRateLimit()
    
    RegistrationService->>RegistrationService: generateOtp()
    
    RegistrationService->>RedisTemplate: set(otpKey, otp, OTP_TTL)
    RegistrationService->>RedisTemplate: set(rateKey, "", OTP_RATE_TTL)
    
    RegistrationService->>EmailService: sendOtpEmail(email, otp, type)

    RegistrationService-->>Client: void
    deactivate RegistrationService
```

---

## 4. TokenServiceImpl

### 4.1. Tạo và Lưu Token (`generateAndSaveTokens`)

```mermaid
sequenceDiagram
    participant Caller
    participant TokenService
    participant JwtUtil
    participant TokenRepository

    Caller->>TokenService: generateAndSaveTokens(user, deviceInfo)
    activate TokenService

    TokenService->>JwtUtil: generateAccessToken(user)
    JwtUtil-->>TokenService: accessToken
    
    TokenService->>JwtUtil: generateRefreshToken(user)
    JwtUtil-->>TokenService: refreshToken
    
    TokenService->>TokenRepository: save(TokenEntity)
    
    TokenService-->>Caller: AuthResponse(accessToken, refreshToken)
    deactivate TokenService
```

### 4.2. Làm mới (`refresh`)

```mermaid
sequenceDiagram
    participant Client
    participant TokenService
    participant JwtUtil
    participant TokenRepository

    Client->>TokenService: refresh(RefreshTokenRequest)
    activate TokenService

    TokenService->>JwtUtil: parseToken(request.token)
    alt Chữ ký token không hợp lệ
        JwtUtil-->>TokenService: Exception
        TokenService-->>Client: throw AppException(REFRESH_TOKEN_INVALID)
    end

    TokenService->>TokenRepository: findByRefreshToken(token)
    alt RT không tìm thấy
        TokenRepository-->>TokenService: Optional.empty()
        TokenService-->>Client: throw AppException(REFRESH_TOKEN_INVALID)
    else Tìm thấy RT
        TokenRepository-->>TokenService: TokenEntity
    end
    
    alt RT đã hết hạn
        TokenService->>TokenRepository: delete(TokenEntity)
        TokenService-->>Client: throw AppException(REFRESH_TOKEN_INVALID)
    end

    TokenService->>TokenRepository: delete(TokenEntity)
    
    TokenService->>TokenService: generateAndSaveTokens(user, null)
    
    TokenService-->>Client: AuthResponse
    deactivate TokenService
```

### 4.3. Kiểm tra (`introspect`)

```mermaid
sequenceDiagram
    participant Client
    participant TokenService
    participant JwtUtil
    participant RedisTemplate

    Client->>TokenService: introspect(IntrospectRequest)
    activate TokenService

    alt Parse thất bại
        TokenService->>JwtUtil: parseToken(request.token)
        JwtUtil-->>TokenService: Exception
        TokenService-->>Client: IntrospectResponse(valid=false)
    else Parse thành công
        TokenService->>JwtUtil: parseToken(request.token)
        JwtUtil-->>TokenService: SignedJWT (và trích xuất jti)
        
        TokenService->>RedisTemplate: hasKey(blacklistAtKey(jti))
        RedisTemplate-->>TokenService: blacklisted (đã bị đưa vào danh sách đen) (true/false)
        
        TokenService-->>Client: IntrospectResponse(valid = !blacklisted)
    end
    deactivate TokenService
```
