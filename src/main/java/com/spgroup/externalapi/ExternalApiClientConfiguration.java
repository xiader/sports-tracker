package com.spgroup.externalapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "external-api")
public class ExternalApiClientConfiguration {

    @Value("${external-api.url}")
    private String url;
    @Value("${external-api.timeout}")
    private Duration timeout;
    @Value("${external-api.retry-attempts}")
    private Integer retryAttempts;
}
