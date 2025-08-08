package com.saltyfish.backend.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "ddzproject.backend")
@Data
public class BackendProperties {
    private String message;
}
