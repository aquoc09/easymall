package com.quocnva.easymall.config;

/**
 * CORS configuration đã được chuyển vào SecurityConfig.corsConfigurationSource().
 *
 * Lý do: Spring Security filter chain xử lý trước Spring MVC.
 * Nếu dùng WebMvcConfigurer.addCorsMappings() thì preflight OPTIONS request
 * sẽ bị chặn 403 tại Security layer trước khi đến MVC layer.
 *
 * Xem: SecurityConfig.java#corsConfigurationSource()
 *
 * @deprecated Class này không còn dùng nữa. Giữ lại để tham chiếu.
 */
@Deprecated
public class CorsConfig {
    // Intentionally empty — logic đã move sang SecurityConfig
}
