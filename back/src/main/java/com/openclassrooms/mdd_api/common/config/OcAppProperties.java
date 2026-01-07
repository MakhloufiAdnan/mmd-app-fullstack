package com.openclassrooms.mdd_api.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "oc.app")
public class OcAppProperties {

    @NotBlank
    private String jwtSecret;

    @Min(1000)
    private long jwtExpirationMs;

    @Min(1000)
    private long refreshTokenExpirationMs;

    /**
     * Dev HTTP => false. Prod HTTPS => true.
     */
    private boolean cookieSecure = false;
}
