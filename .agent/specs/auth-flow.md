# Specification: Authentication Flow (Login & Register)

## 1. Tổng quan (Overview)
- **Cơ chế:** Sử dụng JWT (JSON Web Token) dạng Stateless.
- **Quy trình:**
  1. Người dùng đăng ký (Register) -> Trả về thông báo thành công.
  2. Người dùng đăng nhập (Login) -> Backend trả về `accessToken`.
  3. Frontend lưu `accessToken` và tự động đính kèm vào Header (`Authorization: Bearer <token>`) cho các request sau.

## 2. Nhiệm vụ Backend (Spring Boot)
- **DTOs:**
  - `LoginRequest` (email/username, password)
  - `RegisterRequest` (email, username, password, confirmPassword)
  - `AuthResponse` (token)
- **Endpoints:**
  - `POST /auth/register`: Validate input, hash password, lưu vào DB.
  - `POST /auth/login`: So sánh password, sinh ra JWT token.
- **Security Config:** Public 2 endpoint trên, các endpoint khác (ví dụ `/api/v1/users/me`) yêu cầu xác thực JWT.

## 3. Nhiệm vụ Frontend (ReactJS)
- **UI Components (`src/features/auth/`):**
  - Tạo `LoginForm` và `RegisterForm` sử dụng TailwindCSS.
  - Gọi API từ `src/services/authService.js`.
- **State Management (Zustand hoặc Context):**
  - Tạo store lưu thông tin `user` và `isAuthenticated`.
  - Lưu JWT vào `localStorage` (hoặc `sessionStorage`).
- **Axios Interceptor (`src/services/axiosClient.js`):**
  - Tự động lấy token từ `localStorage` và đính vào Header của mọi request gửi đi.