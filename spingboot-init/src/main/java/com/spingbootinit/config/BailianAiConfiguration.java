package com.spingbootinit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BailianAiConfiguration.class)
@ConfigurationProperties(prefix = "bailian.ai")
public class BailianAiConfiguration {
}
