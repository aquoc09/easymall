# Authentication System — Implementation Plan

## Acknowledgment of Core Directives

Before proceeding, strict adherence to `auth-flow.md` is confirmed:

1. ✅ **Email/Password only** — No Social Login (OAuth2/Google/Facebook).
2. ✅ **OTP-based flows only** — Redis-cached 6-digit OTP (5-min TTL) for registration & password reset. No token-link email.
3. ✅ **Dual-Token + Rotation** — Short-lived AT (~15 min), long-lived RT (~7 days), rotation on refresh.
4. ✅ **Redis Blacklist** — Revoke AT via Redis on logout. No DB whitelist for active JWTs.
5. ✅ **Thin Controllers** — All business logic, token ops, Redis ops live in `AuthServiceImpl`.

---

## Background

The project scaffold already exists (Spring Boot 4.x, PostgreSQL, Redis, Spring Security). The existing code defines:
- Entities: `UserEntity`, `TokenEntity`, `RoleEntity`
- Repositories: `UserRepository`, `TokenRepository`, `RoleRepository`
- Skeleton: `AuthController`, `AuthService`, `AuthServiceImpl`, `EmailService`, `EmailServiceImpl`
- Existing DTOs: `LoginRequest`, `RegisterRequest`, `ActivateAccountRequest`, `IntrospectRequest`, `RefreshTokenRequest`, `AuthResponse`, `IntrospectResponse`, `ApiResponse`
- Exception infra: `AppException`, `ErrorCode`, `GlobalExceptionHandler`
- Config stubs: `SecurityConfig`, `RedisConfig`

**Key observations:**
- `TokenEntity` stores refresh tokens in DB — used for RT persistence and rotation. The Access Token blacklist goes to **Redis only**.
- `pom.xml` includes `spring-boot-starter-security-oauth2-resource-server` which provides `spring-security-oauth2-jose` (Nimbus JOSE). Used for JWT parsing/validation — **not** for OAuth2 social login.
- `spring-boot-starter-data-redis` + `spring-boot-starter-mail` are already present.
- **`UserEntity` has no `password` field** — a Flyway migration must add it.

> [!IMPORTANT]
> `application.yaml` references Google OAuth2 client config. Per directives, Social Login is NOT implemented. These config entries will be **removed** (they cause auto-config failures since no actual OAuth2 flow is wired).

> [!WARNING]
> `UserEntity` has `facebookAccountId` and `googleAccountId` legacy fields. These are left untouched in the DB schema/entity but will NOT be used in auth logic.

---

## Open Questions

> [!IMPORTANT]
> **jwt.valid-duration & jwt.refreshable-duration** are currently `60000ms` (1 min) and `360000ms` (6 min) — clearly dev-mode values. I will update to `900000ms` (15 min) and `604800000ms` (7 days) respectively. Please confirm or override.

---

## Proposed Changes

---

### Database — Flyway Migration

#### [NEW] `V1.6__add_password_to_users.sql`
- Adds `password VARCHAR(255) NOT NULL DEFAULT ''` to `users` table (default allows existing rows; auth will require it non-empty at app level).

#### [MODIFY] `V1.2__create_tokens_table.sql` → leave as-is
- `tokens` table already has the necessary columns. We add `expires_at` and `device_info` via a new migration.

#### [NEW] `V1.7__alter_tokens_add_expiry_device.sql`
- Adds `expires_at TIMESTAMPTZ` and `device_info VARCHAR(255)` to `tokens` table.

---

### Entity Layer

#### [MODIFY] [UserEntity.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/entity/UserEntity.java)
- Add `private String password;` with `@Column(name="password", nullable=false, length=255)`.

#### [MODIFY] [TokenEntity.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/entity/TokenEntity.java)
- Add `private OffsetDateTime expiresAt;` — RT expiry for TTL-aware queries.
- Add `private String deviceInfo;` — device/IP tracking per auth-flow spec.

---

### Configuration Layer

#### [MODIFY] [RedisConfig.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/config/RedisConfig.java)
- Declare `RedisTemplate<String, String>` bean with `StringRedisSerializer` for keys and values.

#### [MODIFY] [SecurityConfig.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/config/SecurityConfig.java)
- `SecurityFilterChain`: disable CSRF, stateless session, permit `/api/v1/auth/**` public, require auth for all else.
- Add custom `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- `PasswordEncoder` bean (`BCryptPasswordEncoder`).
- `AuthenticationManager` bean.

#### [NEW] `JwtConfig.java`
- `@ConfigurationProperties(prefix = "jwt")` binding `signerKey`, `validDuration`, `refreshableDuration`.

#### [MODIFY] [application.yaml](file:///d:/Study/DoAn/DATN/easymall/src/main/resources/application.yaml)
- Update `jwt.valid-duration: 900000` (15 min).
- Update `jwt.refreshable-duration: 604800000` (7 days).
- Remove `spring.security.oauth2.client` block and `login-url.google` (no social login).

---

### New Request DTOs

#### [NEW] `LogoutRequest.java`
- `@NotBlank String accessToken`, `@NotBlank String refreshToken`.

#### [NEW] `ForgotPasswordRequest.java`
- `@NotBlank @Email String email`.

#### [NEW] `ResetPasswordRequest.java`
- `@NotBlank @Email String email`, `@NotBlank @Size(min=6,max=6) String otp`, `@NotBlank @Size(min=8) String newPassword`.

#### [NEW] `ResendOtpRequest.java`
- `@NotBlank @Email String email`, `OtpType type`.

---

### Response DTOs

#### [MODIFY] [AuthResponse.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/dtos/response/AuthResponse.java)
- Replace `token/authenticated` with `accessToken`, `refreshToken`, `tokenType = "Bearer"`.

---

### New Enums

#### [NEW] `OtpType.java` (in `enums/`)
- `ACTIVATION`, `FORGOT_PASSWORD`

---

### Utility / Infrastructure Layer

#### [NEW] `JwtUtil.java` (in `util/`)
- `generateAccessToken(UserEntity)` — JWT with `sub`, `jti` (UUID), `iss`, `iat`, `exp`, `scope` (role), `type=ACCESS`. Signed with HMAC-SHA256.
- `generateRefreshToken(UserEntity)` — JWT with `jti`, `type=REFRESH`.
- `parseToken(String)` — verifies signature, returns `SignedJWT` / claims map.
- `getJti(String)` — extract JTI claim.
- `getRemainingTtlSeconds(String)` — `exp` minus `now`.
- Uses `com.nimbusds.jose.crypto.MACSigner` / `MACVerifier` (on classpath via `spring-security-oauth2-jose`).

#### [NEW] `OtpUtil.java` (in `util/`)
- `generateOtp()` — `String.format("%06d", random.nextInt(1_000_000))`.

#### [NEW] `RedisKeyUtil.java` (in `util/`)
Key builder constants:
- `otp:{TYPE}:{email}` — OTP value
- `pending_user:{email}` — JSON-serialized pending registration data
- `blacklist:at:{jti}` — AT blacklist
- `otp_rate:{TYPE}:{email}` — rate limiting sentinel

#### [NEW] `JwtAuthenticationFilter.java` (in `config/filter/`)
- `OncePerRequestFilter`.
- Extracts `Authorization: Bearer <token>` → `JwtUtil.parseToken()` → checks `blacklist:at:{jti}` in Redis → on success, sets `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`.

---

### Service Layer

#### [MODIFY] [AuthService.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/service/AuthService.java)
```java
AuthResponse login(LoginRequest request);
IntrospectResponse introspect(IntrospectRequest request);
void logout(LogoutRequest request);
AuthResponse refresh(RefreshTokenRequest request);
void register(RegisterRequest request);
void activateAccount(ActivateAccountRequest request);
void resendOtp(ResendOtpRequest request, String clientIp);
void forgotPassword(ForgotPasswordRequest request, String clientIp);
void resetPassword(ResetPasswordRequest request);
UserResponse getCurrentUser();
```

#### [MODIFY] [AuthServiceImpl.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/service/impl/AuthServiceImpl.java)
- **login**: find user by email → verify `isActive` → BCrypt match → generate AT+RT → save `TokenEntity` → return `AuthResponse`
- **introspect**: parse AT → check Redis blacklist → return `{valid}`
- **logout**: parse AT → get JTI + remaining TTL → `SET blacklist:at:{jti} "" EX {ttl}` → delete RT from `tokens` table
- **refresh**: verify RT JWT → find `TokenEntity` by RT value → check not revoked/expired → generate new AT+RT → delete old `TokenEntity` → save new one → return `AuthResponse`
- **register**: `existsByEmail` check → generate OTP → serialize pending user JSON → `SET pending_user:{email} {json} EX 300` + `SET otp:ACTIVATION:{email} {otp} EX 300` → `emailService.sendOtpEmail()`
- **activateAccount**: get `pending_user:{email}` from Redis → get `otp:ACTIVATION:{email}` → compare OTP → deserialize + persist `UserEntity` (BCrypt password) → delete both Redis keys
- **resendOtp**: check `otp_rate:{TYPE}:{email}` in Redis (if exists → throw `OTP_ALREADY_SENT`) → generate OTP → update Redis → `SET otp_rate:{TYPE}:{email} "" EX 60` → send email
- **forgotPassword**: `findByEmail` → check `otp_rate:FORGOT_PASSWORD:{email}` → generate OTP → `SET otp:FORGOT_PASSWORD:{email} {otp} EX 300` → `SET otp_rate:...` → send email
- **resetPassword**: get `otp:FORGOT_PASSWORD:{email}` → compare → find user → encode new password → `save()` → delete OTP key
- **getCurrentUser**: `SecurityContextHolder.getContext().getAuthentication().getName()` → `userRepository.findByEmail()` → `userMapper.toResponse()`

#### [MODIFY] [EmailService.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/service/EmailService.java)
```java
void sendOtpEmail(String toEmail, String otp, OtpType type);
```

#### [MODIFY] [EmailServiceImpl.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/service/impl/EmailServiceImpl.java)
- Uses `JavaMailSender` + `SimpleMailMessage`.
- Subject/body vary by `OtpType`.

---

### Repository Layer

#### [MODIFY] [TokenRepository.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/repository/TokenRepository.java)
Add:
- `Optional<TokenEntity> findByRefreshToken(String refreshToken)` — RT lookup.
- `void deleteByRefreshToken(String refreshToken)` — RT deletion on logout.

---

### Controller Layer

#### [MODIFY] [AuthController.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/controller/AuthController.java)
10 thin endpoints — all return `ApiResponse<T>`:

| Method | Path | Returns |
|--------|------|---------|
| POST | `/api/v1/auth/register` | `ApiResponse<Void>` |
| POST | `/api/v1/auth/active` | `ApiResponse<Void>` |
| POST | `/api/v1/auth/resend-otp` | `ApiResponse<Void>` |
| POST | `/api/v1/auth/login` | `ApiResponse<AuthResponse>` |
| POST | `/api/v1/auth/logout` | `ApiResponse<Void>` |
| POST | `/api/v1/auth/refresh` | `ApiResponse<AuthResponse>` |
| POST | `/api/v1/auth/introspect` | `ApiResponse<IntrospectResponse>` |
| POST | `/api/v1/auth/forgot-password` | `ApiResponse<Void>` |
| POST | `/api/v1/auth/reset-password` | `ApiResponse<Void>` |
| GET | `/api/v1/auth/me` | `ApiResponse<UserResponse>` |

---

### Exception & Error Codes

#### [MODIFY] [ErrorCode.java](file:///d:/Study/DoAn/DATN/easymall/src/main/java/com/quocnva/easymall/exception/ErrorCode.java)
Add:
- `RATE_LIMIT_EXCEEDED(1007, "Too many requests, please try again later", TOO_MANY_REQUESTS)`
- `REFRESH_TOKEN_INVALID(1008, "Refresh token is invalid or expired", UNAUTHORIZED)`
- `PENDING_REGISTRATION_NOT_FOUND(1009, "No pending registration found for this email", NOT_FOUND)`
- `ACCOUNT_ALREADY_ACTIVE(1010, "Account is already active", CONFLICT)`

---

## Verification Plan

### Build Verification
```bash
./mvnw clean compile
```

### Manual API Test Flow
1. `POST /register` → email with OTP
2. `POST /active` with OTP → user in DB
3. `POST /login` → `{accessToken, refreshToken}`
4. `GET /me` with `Authorization: Bearer {accessToken}` → user info
5. `POST /introspect` → `{valid: true}`
6. `POST /logout` → 200 OK
7. `POST /introspect` same AT → `{valid: false}`
8. `POST /refresh` with old RT → `401` (RT deleted)
9. `POST /forgot-password` → OTP email
10. `POST /reset-password` with OTP + new password → 200
11. `POST /resend-otp` twice in 60s → second returns `429`
