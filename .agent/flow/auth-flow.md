# Authentication Flow

This document describes the upgraded authentication flow used in this backend project, focusing on a secure, scalable JWT-based architecture with Redis integration.

## 1) Overview

The authentication system is a dual-token based system (Access Token & Refresh Token) using JWT. It utilizes Redis for OTP caching, rate limiting, and token revocation (Blacklist).

It supports these main operations:

- login (issues AT & RT)
- token introspection
- logout (token revocation via Redis Blacklist)
- refresh token (token rotation)
- get current authenticated user
- register (with Redis OTP)
- active account
- resend OTP
- forgot password (with Redis OTP)
- reset password

The flow is built around:

- `AuthenticationController`
- `AuthenticationService`
- `AuthenticationServiceImp`
- User persistence (Database)
- Token & OTP persistence (Redis)
- Spring Security context

## 2) Main Authentication Endpoints

### Login

`POST /auth/login`

Purpose:

- Verify user credentials.
- Generate a short-lived Access Token (e.g., 15 mins) and a long-lived Refresh Token (e.g., 7 days).
- Bind the Refresh Token to the user's device/IP for security tracking.

Expected behavior:

- The user is looked up by email/username.
- Password is checked against the stored encoded password.
- If valid, generate Access Token and Refresh Token.
- Tokens are returned to the client (preferably via `HttpOnly` cookies for maximum security against XSS, or via response body).

### Introspect

`POST /auth/introspect`

Purpose:

- Check whether an Access Token is valid.

Expected behavior:

- Reject blank tokens.
- Parse the token and verify the signing algorithm and signature.
- Check expiration time.
- **Check against Redis Blacklist** to ensure the token hasn't been revoked.
- Return whether the token is active/valid.

### Logout

`POST /auth/logout`

Purpose:

- Invalidate the current tokens.

Expected behavior:

- Parse the tokens (Access and Refresh).
- Add the Access Token's JTI (JWT ID) to the **Redis Blacklist** with a TTL equal to its remaining expiration time.
- Invalidate the Refresh Token in the database/Redis.
- Return success status.

### Refresh Token

`POST /auth/refresh`

Purpose:

- Issue a new Access Token when the old one expires, without requiring credentials.

Expected behavior:

- Verify the Refresh Token signature and expiration.
- Check if the Refresh Token is valid and matches the user's device footprint.
- **Implement Token Rotation:** Invalidate the used Refresh Token and generate a new pair of Access Token and Refresh Token.
- Return the new tokens.

### Register

`POST /auth/register`

Purpose:

- Initiate the registration process for a new user.

Expected behavior:

- Verify if the username/email already exists. If yes, return an error.
- Generate a 6-digit OTP.
- Store the OTP in **Redis** with the user's pending credentials and a 5-minute expiration (TTL).
- Send the OTP to the user's email via the Mail Service.
- Return success message (registration pending activation).

### Resend OTP

`POST /auth/resend-otp`

Purpose:

- Resend an activation or password-reset OTP if the previous one expired or was lost.

Expected behavior:

- Verify the user's request.
- Apply **Rate Limiting** (e.g., max 1 request per minute per IP) to prevent email spamming.
- Generate a new 6-digit OTP, update Redis with a new 5-minute TTL, and send the email.

### Active Account

`POST /auth/active`

Purpose:

- Verify OTP and finalize account creation.

Expected behavior:

- Retrieve the pending registration data and OTP from Redis using the provided email/username.
- Validate the OTP. If invalid or expired, return an error.
- If valid, persist the user into the main database.
- Clear the OTP from Redis.
- Return success status.

### Forgot Password

`POST /auth/forgot-password`

Purpose:

- Initiate the password recovery process via OTP.

Expected behavior:

- Look up the user by email.
- If the user exists, generate a 6-digit OTP.
- Store the OTP in **Redis** linked to the user's email with a 5-minute TTL.
- Send the OTP to the user's email.
- Apply rate limiting to prevent spam.

### Reset Password

`POST /auth/reset-password`

Purpose:

- Verify the OTP and set a new password.

Expected behavior:

- Receive email, OTP, and the new password.
- Validate the OTP against the value stored in Redis.
- If valid, encode the new password and update the user's record in the database.
- Clear the OTP from Redis.
- Return success status.

## 3) Authentication Flow Step-by-Step

### A. Login Flow

1. Client sends credentials to `/auth/login`.
2. Service validates credentials against the database.
3. Access Token (AT) and Refresh Token (RT) are generated.
4. Client receives AT and RT.

### B. Token Validation Flow

1. Client includes AT in the `Authorization` header for protected API calls.
2. Spring Security filter intercepts the request.
3. Token signature and expiration are verified.
4. **Redis check:** Ensure the token's JTI is _not_ in the Blacklist.
5. If valid, Spring Security context is populated, and the request proceeds.

### C. Logout Flow

1. Client calls `/auth/logout` with their current AT and RT.
2. Service extracts the AT's ID and remaining lifespan.
3. AT is placed in the Redis Blacklist until it naturally expires.
4. RT is deleted/invalidated.
5. Subsequent requests using these tokens will fail.

### D. Password Recovery Flow (OTP-based)

1. User requests password reset via email.
2. System generates OTP, caches it in Redis (5 mins TTL), and emails the user.
3. User submits the OTP along with a new password.
4. System verifies OTP in Redis. If matched, updates the password and deletes the Redis cache.

## 4) Token Structure

The token includes important claims such as:

- `sub`: username/email
- `jti`: unique token identifier (crucial for Redis Blacklist)
- `iss`: issuer
- `iat`: issue time
- `exp`: expiration time
- `scope`: user roles/authorities
- `type`: "ACCESS" or "REFRESH"

## 5) Security Behavior & Fortifications

- **Blacklist over Whitelist:** Valid tokens are not queried from the DB on every request. Only revoked tokens are checked against Redis, ensuring high performance.
- **Rate Limiting:** Redis is used to limit failed login attempts (preventing Brute Force) and OTP generation requests (preventing email spam).
- **Token Rotation:** Using a refresh token invalidates it and issues a new one, mitigating the risk of stolen refresh tokens.
- **Standard Authentication Only:** The system strictly utilizes standard Email/Password authentication.

## 6) Current User Access

The application can resolve the current logged-in user from the security context.

Behavior:

1. Read the authenticated principal from the security context.
2. Extract the username.
3. Load the user from the database.
4. Return the user record.

## 7) Error Handling Rules

Authentication should fail safely when:

- Username does not exist or password is incorrect.
- Token is blank, expired, or signature is invalid.
- Token is found in the Redis Blacklist.
- OTP is incorrect or expired (not found in Redis).
- Rate limit threshold is exceeded.

Errors must be returned in a consistent, standardized JSON response format.

## 8) Implementation Notes

- Keep authentication logic in the service layer; keep controller methods thin.
- Utilize Spring Data Redis for fast Blacklist lookups and OTP caching.
- Ensure proper transactional boundaries when registering users and clearing Redis caches.

## 9) Agent Guidance

When working on authentication in this project:

- Follow the defined OTP-based flows using Redis; **do not** implement link-based password resets.
- Preserve the Blacklist token lifecycle.
- Update token persistence and security rules together.
- Maintain the decision to use standard Email/Password authentication without introducing third-party or social login complexities.

## 10) Goal

The authentication flow should remain secure, high-performing, easy to maintain, and completely consistent with the existing Spring Boot and Redis architecture.
