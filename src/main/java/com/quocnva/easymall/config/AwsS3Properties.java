package com.quocnva.easymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds AWS S3 properties from application.yaml (prefix: aws).
 *
 * <pre>
 * aws:
 *   s3:
 *     bucket-name: ${AWS_BUCKET}
 *     region:      ${AWS_DEFAULT_REGION}
 *   credentials:
 *     access-key:  ${AWS_ACCESS_KEY_ID}
 *     secret-key:  ${AWS_SECRET_ACCESS_KEY}
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsS3Properties {

    private S3 s3 = new S3();
    private Credentials credentials = new Credentials();

    @Getter
    @Setter
    public static class S3 {
        private String bucketName;
        private String region;
    }

    @Getter
    @Setter
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }

    /**
     * Base URL công khai của S3 bucket.
     * Ví dụ: {@code https://shopco-s3.s3.ap-southeast-1.amazonaws.com}
     * Bind từ {@code aws.base-url} trong application.yaml.
     */
    private String baseUrl;
}
