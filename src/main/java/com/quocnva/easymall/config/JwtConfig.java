package com.quocnva.easymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    /** HMAC-SHA256 signing key — loaded from JWT_SIGNER_KEY env var. */
    private String signerKey;

    /** Access Token lifetime in milliseconds (default: 15 min). */
    private long validDuration;

    /** Refresh Token lifetime in milliseconds (default: 7 days). */
    private long refreshableDuration;
}
