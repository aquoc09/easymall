package com.quocnva.easymall.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds GHN integration properties from application.yaml (prefix: ghn).
 *
 * <pre>
 * ghn:
 *   token: ${GHN_TOKEN}
 *   url:   ${GHN_API_URL}
 *   shop-id:   ${GHN_SHOP_ID}
 *   ward-code: ${GHN_WARD_CODE}   # ward code của kho hàng
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "ghn")
@Getter
@Setter
public class GhnProperties {

    private String token;
    private String url;
    private Integer shopId;

    /**
     * Ward code của kho gửi hàng (lấy từ GHN dashboard).
     * Được dùng để tra cứu districtId kho qua GhnMasterDataService.
     */
    private String wardCode;

    /**
     * District ID của kho gửi hàng.
     */
    private Integer districtId;
}
