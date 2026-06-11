# Authentication Flow

This document describes the authentication flow used in this backend project.

## 1) Overview

The authentication system is token-based and uses JWT-style access tokens.
It supports these main operations:

- login
- token introspection
- logout
- refresh token
- get current authenticated user
- register
- active account

The flow is built around:

- `AuthenticationController`
- `AuthenticationService`
- `AuthenticationServiceImp`
- user persistence
- token persistence
- Spring Security context

## 2) Main Authentication Endpoints

### Login

`POST /auth/login`

Purpose:

- Verify user credentials
- Generate a signed token
- Persist token data
- Return authentication result

Expected behavior:

- The user is looked up by username
- Password is checked against the stored encoded password
- If valid, a token is generated
- The token is stored so it can be managed later
- The response contains the token and authentication status

### Introspect

`POST /auth/introspect`

Purpose:

- Check whether a token is valid
- Verify signature and expiration
- Return token status without logging the user in again

Expected behavior:

- Reject blank tokens
- Parse the token
- Verify the signing algorithm
- Verify the signature
- Check expiration time
- Return whether the token is active/valid

### Logout

`POST /auth/logout`

Purpose:

- Invalidate the current token

Expected behavior:

- Parse the token
- Locate the persisted token record
- Remove it from storage
- Return success if the token was removed

### Refresh Token

`POST /auth/refresh`

Purpose:

- Issue a new token when the old one is still within the refresh window

Expected behavior:

- Verify the token signature
- Check refresh eligibility based on token issue time
- Load the user from the token subject
- Create a new token
- Replace the old token record with the new one
- Return the new token

### Register

`POST /auth/register`

Purpose:

- Register a new user
- Verify user credentials
- Using redis to generate OTP code with 6 digits
- Using mail service to send OTP code to user
- Using redis to store OTP code with user credentials and expired time 5 minutes

Expected behavior:

- The user is looked up by username or email
- If the user is exists, return error
- Generate OTP code and store it in redis
- Send OTP code to user's email
- Return success message

### Active account

`POST /auth/active`

Purpose:

- Activate the account
- Verify user credentials
- Generate a signed token
- Persist token data
- Return authentication result

Expected behavior:

- The user is looked up by username
- Password is checked against the stored encoded password
- If valid, a token is generated
- The token is stored so it can be managed later
- The response contains the token and authentication status

## 3) Authentication Flow Step-by-Step

### A. Login Flow

1. Client sends username and password to the login endpoint.
2. Service loads the user by username.
3. Password is validated.
4. If valid:
   - a new token is generated
   - token claims are built
   - token is signed
   - token information is saved in the database
5. Client receives the token.

### B. Token Validation Flow

1. Client sends a token to the introspection endpoint.
2. Service checks whether the token is empty.
3. Token header and signature are verified.
4. Expiration is checked.
5. Service returns whether the token is valid.

### C. Logout Flow

1. Client sends a token to logout.
2. Token is parsed and verified.
3. The token record is found in storage.
4. The token record is deleted.
5. The token is no longer usable.

### D. Refresh Flow

1. Client sends a token to refresh.
2. Service verifies the token and checks refresh eligibility.
3. If the refresh window is still valid:
   - a new token is generated
   - old token data is removed
   - new token data is stored
4. Client receives the new token.

## 4) Token Structure

The token includes important claims such as:

- subject: username
- token id
- issuer
- issue time
- expiration time
- scope/roles
- refresh token value

This allows the application to:

- identify the user
- verify token ownership
- check permissions
- manage refresh and logout behavior

## 5) Security Behavior

- Tokens are signed using a server-side secret.
- Token verification checks both signature and expiration.
- Authentication depends on the current user being present and valid in storage.
- Current user access relies on the Spring Security context.
- Protected operations should only run for authenticated requests.

## 6) Current User Access

The application can resolve the current logged-in user from the security context.

Behavior:

1. Read the authenticated principal from the security context.
2. Extract the username.
3. Load the user from the database.
4. Return the user record.

This is useful for:

- profile actions
- role-based operations
- user-specific business logic

## 7) Error Handling Rules

Authentication should fail safely when:

- username does not exist
- password is incorrect
- token is blank
- token signature is invalid
- token is expired
- token cannot be parsed
- user referenced by token no longer exists

Errors should be returned in a consistent project-specific format.

## 8) Implementation Notes

- Keep authentication logic in the service layer.
- Keep controller methods thin.
- Store token state when the project requires logout and refresh control.
- Maintain consistency between token claims, persistence, and security checks.
- Do not weaken token verification logic unless explicitly required.

## 9) Agent Guidance

When working on authentication in this project:

- follow the existing endpoint structure
- preserve the current token lifecycle
- update token persistence and security rules together
- check the impact on refresh, logout, and current-user behavior
- avoid changing security logic without understanding downstream effects

## 10) Goal

The authentication flow should remain:

- secure
- predictable
- easy to maintain
- consistent with the existing Spring backend architecture
