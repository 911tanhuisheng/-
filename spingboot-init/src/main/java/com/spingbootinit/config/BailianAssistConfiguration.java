package com.spingbootinit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BailianAssistProperties.class)
public class BailianAssistConfiguration {
}
