package com.spingbootinit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vision-ai")
public class VisionAiProperties {
    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8090";
}
