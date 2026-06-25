package com.quocnva.easymall.config.filter;

import com.nimbusds.jwt.SignedJWT;
import com.quocnva.easymall.util.JwtUtil;
import com.quocnva.easymall.util.RedisKeyUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Intercepts every request once, extracts the Bearer AT from the Authorization
 * header,
 * validates it (signature + expiry + Redis blacklist), and populates the
 * SecurityContext.
 *
 * If the token is missing or invalid, the filter simply continues without
 * authentication.
 * Spring Security's authorization rules then reject the request if the endpoint
 * requires auth.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                SignedJWT parsed = jwtUtil.parseToken(token);
                String jti = parsed.getJWTClaimsSet().getJWTID();
                Date exp = parsed.getJWTClaimsSet().getExpirationTime();
                String email = parsed.getJWTClaimsSet().getSubject();
                String scope = (String) parsed.getJWTClaimsSet().getClaim("scope");

                boolean notExpired = exp != null && exp.after(new Date());
                boolean notBlacklisted = Boolean.FALSE.equals(
                        redisTemplate.hasKey(RedisKeyUtil.blacklistAtKey(jti)));

                if (notExpired && notBlacklisted) {
                    List<SimpleGrantedAuthority> authorities = StringUtils.hasText(scope)
                            ? List.of(new SimpleGrantedAuthority(scope))
                            : List.of();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null,
                            authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // Invalid token — continue unauthenticated; SecurityConfig handles rejection
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
